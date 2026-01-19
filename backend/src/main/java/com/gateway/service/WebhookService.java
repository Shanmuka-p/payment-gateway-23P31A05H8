package com.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gateway.model.Merchant;
import com.gateway.model.WebhookLog;
import com.gateway.queue.RedisQueueService;
import com.gateway.repo.WebhookLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookService {
    private final WebhookLogRepository repository;
    private final RedisQueueService queueService;
    private final ObjectMapper mapper;

    public WebhookService(WebhookLogRepository repository, RedisQueueService queueService) {
        this.repository = repository;
        this.queueService = queueService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Transactional
    public void enqueueWebhook(Merchant merchant, String event, Map<String, Object> payload) {
        if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isBlank()) {
            return;
        }
        try {
            WebhookLog log = new WebhookLog();
            log.setId(UUID.randomUUID());
            log.setMerchantId(merchant.getId());
            log.setEvent(event);
            log.setPayload(mapper.writeValueAsString(payloadEnvelope(event, payload)));
            log.setStatus("pending");
            log.setAttempts(0);
            // Set nextRetryAt so the scheduler can pick this up even if the queue delivery is missed (e.g., after restart)
            log.setNextRetryAt(OffsetDateTime.now());
            log.setCreatedAt(OffsetDateTime.now());
            repository.save(log);

            Map<String, Object> jobPayload = new HashMap<>();
            jobPayload.put("webhookId", log.getId().toString());
            queueService.enqueue(RedisQueueService.QUEUE_DELIVER_WEBHOOK, jobPayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue webhook", e);
        }
    }

    public Page<WebhookLog> listForMerchant(UUID merchantId, int offset, int limit) {
        int page = offset / limit;
        return repository.findByMerchantId(merchantId, PageRequest.of(page, limit));
    }

    @Transactional
    public Optional<WebhookLog> resetAndEnqueue(UUID webhookId, UUID merchantId) {
        Optional<WebhookLog> logOpt = repository.findById(webhookId);
        if (logOpt.isEmpty()) return Optional.empty();
        WebhookLog log = logOpt.get();
        if (!log.getMerchantId().equals(merchantId)) {
            return Optional.empty();
        }
        log.setAttempts(0);
        log.setStatus("pending");
        log.setNextRetryAt(null);
        repository.save(log);
        Map<String, Object> jobPayload = new HashMap<>();
        jobPayload.put("webhookId", webhookId.toString());
        queueService.enqueue(RedisQueueService.QUEUE_DELIVER_WEBHOOK, jobPayload);
        return Optional.of(log);
    }

    public List<WebhookLog> findDuePending(OffsetDateTime now) {
        return repository.findByStatusAndNextRetryAtLessThanEqual("pending", now);
    }

    private Map<String, Object> payloadEnvelope(String event, Map<String, Object> body) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("event", event);
        wrapper.put("timestamp", System.currentTimeMillis() / 1000);
        Map<String, Object> data = new HashMap<>();
        if (event.startsWith("refund")) {
            data.put("refund", body);
        } else {
            data.put("payment", body);
        }
        wrapper.put("data", data);
        return wrapper;
    }
}
