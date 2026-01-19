package com.gateway.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.model.Refund;
import com.gateway.queue.RedisQueueService;
import com.gateway.repo.MerchantRepository;
import com.gateway.repo.PaymentRepository;
import com.gateway.repo.RefundRepository;
import com.gateway.service.RefundService;
import com.gateway.service.WebhookService;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "app.worker.enabled", havingValue = "true")
public class RefundWorker {
    private static final Logger log = LoggerFactory.getLogger(RefundWorker.class);

    private final RedisQueueService queueService;
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final RefundService refundService;
    private final WebhookService webhookService;
    private final ObjectMapper mapper;
    private final Random random = new Random();

    @Value("${app.test-mode:false}")
    private boolean testMode;

    @Value("${app.test-processing-delay:1000}")
    private long testDelayMs;

    public RefundWorker(RedisQueueService queueService, RefundRepository refundRepository, PaymentRepository paymentRepository, MerchantRepository merchantRepository, RefundService refundService, WebhookService webhookService) {
        this.queueService = queueService;
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.refundService = refundService;
        this.webhookService = webhookService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        queueService.setWorkerHeartbeat();
        Thread workerThread = new Thread(() -> run(), "RefundWorker");
        workerThread.setDaemon(false);
        workerThread.start();
        log.info("RefundWorker thread started");
    }

    public void run() {
        while (true) {
            try {
                queueService.setWorkerHeartbeat();
                String payloadStr = queueService.blockingPop(RedisQueueService.QUEUE_PROCESS_REFUND, Duration.ofSeconds(5));
                if (payloadStr == null) continue;
                queueService.incrProcessing();
                Map<?, ?> payload = mapper.readValue(payloadStr, Map.class);
                String refundId = (String) payload.get("refundId");
                processRefund(refundId);
                queueService.incrCompleted();
            } catch (Exception e) {
                log.error("RefundWorker error", e);
                queueService.incrFailed();
            } finally {
                queueService.decrProcessing();
            }
        }
    }

    private void processRefund(String refundId) throws InterruptedException {
        Optional<Refund> refundOpt = refundRepository.findById(refundId);
        if (refundOpt.isEmpty()) {
            log.warn("Refund not found: {}", refundId);
            return;
        }
        Refund refund = refundOpt.get();
        Optional<Payment> paymentOpt = paymentRepository.findById(refund.getPaymentId());
        if (paymentOpt.isEmpty()) {
            log.warn("Payment missing for refund {}", refundId);
            return;
        }
        Optional<Merchant> merchantOpt = merchantRepository.findById(refund.getMerchantId());
        if (merchantOpt.isEmpty()) {
            log.warn("Merchant missing for refund {}", refundId);
            return;
        }

        long delay = testMode ? testDelayMs : (3000 + random.nextInt(2001));
        Thread.sleep(delay);

        refund.setStatus("processed");
        refund.setProcessedAt(OffsetDateTime.now());
        refundRepository.save(refund);

        Payment payment = paymentOpt.get();
        if (refund.getAmount() == payment.getAmount()) {
            payment.setStatus("refunded");
            payment.setUpdatedAt(OffsetDateTime.now());
            paymentRepository.save(payment);
        }

        Merchant merchant = merchantOpt.get();
        webhookService.enqueueWebhook(merchant, "refund.processed", refundService.refundPayload(refund));
    }
}
