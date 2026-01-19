package com.gateway.service;

import com.gateway.dto.CreateRefundRequest;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.model.Refund;
import com.gateway.queue.RedisQueueService;
import com.gateway.repo.PaymentRepository;
import com.gateway.repo.RefundRepository;
import com.gateway.util.IdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final RedisQueueService queueService;
    private final WebhookService webhookService;

    public RefundService(RefundRepository refundRepository, PaymentRepository paymentRepository, RedisQueueService queueService, WebhookService webhookService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.queueService = queueService;
        this.webhookService = webhookService;
    }

    @Transactional
    public Map<String, Object> createRefund(Merchant merchant, String paymentId, CreateRefundRequest req) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (!payment.getMerchantId().equals(merchant.getId())) {
            throw new IllegalArgumentException("Payment not found");
        }
        if (!"success".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("Payment not in refundable state");
        }

        long alreadyRefunded = refundRepository.findByPaymentId(paymentId).stream()
                .mapToLong(Refund::getAmount)
                .sum();
        long available = payment.getAmount() - alreadyRefunded;
        if (req.getAmount() > available) {
            throw new IllegalStateException("Refund amount exceeds available amount");
        }

        String refundId = IdUtil.randomId("rfnd_", 16);
        OffsetDateTime now = OffsetDateTime.now();
        Refund refund = new Refund();
        refund.setId(refundId);
        refund.setPaymentId(paymentId);
        refund.setMerchantId(merchant.getId());
        refund.setAmount(req.getAmount());
        refund.setReason(req.getReason());
        refund.setStatus("pending");
        refund.setCreatedAt(now);
        refundRepository.save(refund);

        Map<String, Object> jobPayload = new HashMap<>();
        jobPayload.put("refundId", refundId);
        queueService.enqueue(RedisQueueService.QUEUE_PROCESS_REFUND, jobPayload);

        webhookService.enqueueWebhook(merchant, "refund.created", refundPayload(refund));
        return refundPayload(refund);
    }

    public Map<String, Object> refundPayload(Refund refund) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", refund.getId());
        payload.put("payment_id", refund.getPaymentId());
        payload.put("amount", refund.getAmount());
        payload.put("reason", refund.getReason());
        payload.put("status", refund.getStatus());
        payload.put("created_at", refund.getCreatedAt());
        payload.put("processed_at", refund.getProcessedAt());
        return payload;
    }
}
