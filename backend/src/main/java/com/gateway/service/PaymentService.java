package com.gateway.service;

import com.gateway.dto.CreatePaymentRequest;
import com.gateway.dto.CapturePaymentRequest;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.queue.RedisQueueService;
import com.gateway.repo.PaymentRepository;
import com.gateway.util.IdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final RedisQueueService queueService;
    private final WebhookService webhookService;

    public PaymentService(PaymentRepository paymentRepository, RedisQueueService queueService, WebhookService webhookService) {
        this.paymentRepository = paymentRepository;
        this.queueService = queueService;
        this.webhookService = webhookService;
    }

    @Transactional
    public Map<String, Object> createPayment(Merchant merchant, CreatePaymentRequest req) {
        String paymentId = IdUtil.randomId("pay_", 16);
        OffsetDateTime now = OffsetDateTime.now();

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setMerchantId(merchant.getId());
        payment.setOrderId(req.getOrderId());
        payment.setAmount(50000L);
        payment.setCurrency("INR");
        payment.setMethod(req.getMethod());
        payment.setVpa(req.getVpa());
        payment.setCardLast4(null);
        payment.setStatus("pending");
        payment.setCreatedAt(now);
        paymentRepository.save(payment);
        paymentRepository.flush();

        webhookService.enqueueWebhook(merchant, "payment.created", buildPaymentPayload(payment));
        webhookService.enqueueWebhook(merchant, "payment.pending", buildPaymentPayload(payment));

        Map<String, Object> jobPayload = new HashMap<>();
        jobPayload.put("paymentId", paymentId);
        queueService.enqueue(RedisQueueService.QUEUE_PROCESS_PAYMENT, jobPayload);

        return buildPaymentPayload(payment);
    }

    @Transactional
    public Map<String, Object> capturePayment(Merchant merchant, String paymentId, CapturePaymentRequest req) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (!payment.getMerchantId().equals(merchant.getId())) {
            throw new IllegalArgumentException("Payment not found");
        }
        if (!"success".equalsIgnoreCase(payment.getStatus()) || payment.isCaptured()) {
            throw new IllegalStateException("Payment not in capturable state");
        }
        if (req.getAmount() != null && req.getAmount() > payment.getAmount()) {
            throw new IllegalStateException("Capture amount exceeds authorized amount");
        }
        payment.setCaptured(true);
        payment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);
        webhookService.enqueueWebhook(merchant, "payment.success", buildPaymentPayload(payment));
        return buildPaymentPayload(payment);
    }

    public Optional<Payment> findById(String id) {
        return paymentRepository.findById(id);
    }

    public Map<String, Object> buildPaymentPayload(Payment payment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", payment.getId());
        data.put("order_id", payment.getOrderId());
        data.put("amount", payment.getAmount());
        data.put("currency", payment.getCurrency());
        data.put("method", payment.getMethod());
        data.put("vpa", payment.getVpa());
        data.put("status", payment.getStatus());
        data.put("captured", payment.isCaptured());
        data.put("created_at", payment.getCreatedAt());
        data.put("updated_at", payment.getUpdatedAt());
        data.put("error_code", payment.getErrorCode());
        data.put("error_description", payment.getErrorDescription());
        return data;
    }
}
