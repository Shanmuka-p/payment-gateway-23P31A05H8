package com.gateway.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gateway.model.Merchant;
import com.gateway.model.WebhookLog;
import com.gateway.queue.RedisQueueService;
import com.gateway.repo.MerchantRepository;
import com.gateway.repo.WebhookLogRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.worker.enabled", havingValue = "true")
public class WebhookWorker {
    private static final Logger log = LoggerFactory.getLogger(WebhookWorker.class);

    private final RedisQueueService queueService;
    private final WebhookLogRepository webhookLogRepository;
    private final MerchantRepository merchantRepository;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    @Value("${app.webhook-retry-intervals-test:false}")
    private boolean testIntervals;

    public WebhookWorker(RedisQueueService queueService, WebhookLogRepository webhookLogRepository, MerchantRepository merchantRepository) {
        this.queueService = queueService;
        this.webhookLogRepository = webhookLogRepository;
        this.merchantRepository = merchantRepository;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PostConstruct
    public void init() {
        queueService.setWorkerHeartbeat();
        Thread workerThread = new Thread(() -> run(), "WebhookWorker");
        workerThread.setDaemon(false);
        workerThread.start();
        log.info("WebhookWorker thread started");
    }

    public void run() {
        while (true) {
            try {
                queueService.setWorkerHeartbeat();
                String payloadStr = queueService.blockingPop(RedisQueueService.QUEUE_DELIVER_WEBHOOK, Duration.ofSeconds(5));
                if (payloadStr == null) continue;
                queueService.incrProcessing();
                Map<?, ?> payload = mapper.readValue(payloadStr, Map.class);
                String webhookId = (String) payload.get("webhookId");
                attemptDelivery(UUID.fromString(webhookId));
                queueService.incrCompleted();
            } catch (Exception e) {
                log.error("WebhookWorker error", e);
                queueService.incrFailed();
            } finally {
                queueService.decrProcessing();
            }
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void enqueueDueRetries() {
        List<WebhookLog> due = webhookLogRepository.findByStatusAndNextRetryAtLessThanEqual("pending", OffsetDateTime.now());
        for (WebhookLog log : due) {
            Map<String, Object> job = Map.of("webhookId", log.getId().toString());
            queueService.enqueue(RedisQueueService.QUEUE_DELIVER_WEBHOOK, job);
        }
    }

    private void attemptDelivery(UUID webhookId) {
        Optional<WebhookLog> logOpt = webhookLogRepository.findById(webhookId);
        if (logOpt.isEmpty()) return;
        WebhookLog log = logOpt.get();
        Optional<Merchant> merchantOpt = merchantRepository.findById(log.getMerchantId());
        if (merchantOpt.isEmpty()) return;
        Merchant merchant = merchantOpt.get();
        if (merchant.getWebhookUrl() == null || merchant.getWebhookSecret() == null) {
            markFailed(log, 0, "Webhook not configured");
            return;
        }

        int attempts = log.getAttempts() + 1;
        OffsetDateTime now = OffsetDateTime.now();
        try {
            String signature = computeHmac(log.getPayload(), merchant.getWebhookSecret());
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("X-Webhook-Signature", signature);
            HttpEntity<String> entity = new HttpEntity<>(log.getPayload(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(merchant.getWebhookUrl(), entity, String.class);
            int status = response.getStatusCode().value();
            log.setAttempts(attempts);
            log.setLastAttemptAt(now);
            log.setResponseCode(status);
            log.setResponseBody(response.getBody());
            if (status >= 200 && status < 300) {
                log.setStatus("success");
                log.setNextRetryAt(null);
            } else {
                scheduleRetry(log, attempts, now);
            }
        } catch (Exception ex) {
            System.err.println("Webhook delivery error: " + ex.getMessage());
            scheduleRetry(log, attempts, now);
        }
        webhookLogRepository.save(log);
    }

    private void scheduleRetry(WebhookLog logEntity, int attempts, OffsetDateTime now) {
        logEntity.setAttempts(attempts);
        logEntity.setLastAttemptAt(now);
        if (attempts >= 5) {
            logEntity.setStatus("failed");
            logEntity.setNextRetryAt(null);
            return;
        }
        long nextSeconds = retryDelaySeconds(attempts);
        logEntity.setStatus("pending");
        logEntity.setNextRetryAt(now.plusSeconds(nextSeconds));
    }

    private long retryDelaySeconds(int attempt) {
        if (testIntervals) {
            return switch (attempt) {
                case 1 -> 0;
                case 2 -> 5;
                case 3 -> 10;
                case 4 -> 15;
                default -> 20;
            };
        }
        return switch (attempt) {
            case 1 -> 0;
            case 2 -> 60;
            case 3 -> 300;
            case 4 -> 1800;
            default -> 7200;
        };
    }

    private void markFailed(WebhookLog logEntity, int attempts, String message) {
        logEntity.setStatus("failed");
        logEntity.setAttempts(attempts);
        logEntity.setResponseBody(message);
        webhookLogRepository.save(logEntity);
    }

    private String computeHmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
