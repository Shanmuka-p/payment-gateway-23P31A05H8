package com.gateway.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gateway.model.Merchant;
import com.gateway.model.Payment;
import com.gateway.queue.RedisQueueService;
import com.gateway.repo.MerchantRepository;
import com.gateway.repo.PaymentRepository;
import com.gateway.service.PaymentService;
import com.gateway.service.WebhookService;
import jakarta.annotation.PostConstruct;
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
public class PaymentWorker {
    private static final Logger log = LoggerFactory.getLogger(PaymentWorker.class);

    private final RedisQueueService queueService;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentService paymentService;
    private final WebhookService webhookService;
    private final ObjectMapper mapper;
    private final Random random = new Random();

    @Value("${app.test-mode:false}")
    private boolean testMode;

    @Value("${app.test-processing-delay:1000}")
    private long testDelayMs;

    @Value("${app.test-payment-success:true}")
    private boolean testPaymentSuccess;

    public PaymentWorker(RedisQueueService queueService, PaymentRepository paymentRepository, MerchantRepository merchantRepository, PaymentService paymentService, WebhookService webhookService) {
        this.queueService = queueService;
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.paymentService = paymentService;
        this.webhookService = webhookService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        queueService.setWorkerHeartbeat();
        Thread workerThread = new Thread(() -> run(), "PaymentWorker");
        workerThread.setDaemon(false);
        workerThread.start();
        log.info("PaymentWorker thread started");
    }

    public void run() {
        while (true) {
            try {
                queueService.setWorkerHeartbeat();
                String payloadStr = queueService.blockingPop(RedisQueueService.QUEUE_PROCESS_PAYMENT, Duration.ofSeconds(5));
                if (payloadStr == null) {
                    continue;
                }
                queueService.incrProcessing();
                Map<?, ?> payload = mapper.readValue(payloadStr, Map.class);
                String paymentId = (String) payload.get("paymentId");
                processPayment(paymentId);
                queueService.incrCompleted();
            } catch (Exception e) {
                log.error("PaymentWorker error", e);
                queueService.incrFailed();
            } finally {
                queueService.decrProcessing();
            }
        }
    }

    private void processPayment(String paymentId) throws InterruptedException {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for job: {}", paymentId);
            return;
        }
        Payment payment = paymentOpt.get();
        Optional<Merchant> merchantOpt = merchantRepository.findById(payment.getMerchantId());
        if (merchantOpt.isEmpty()) {
            log.warn("Merchant missing for payment: {}", paymentId);
            return;
        }

        long delay = testMode ? testDelayMs : (5000 + random.nextInt(5001));
        Thread.sleep(delay);

        boolean success = determineOutcome(payment);
        if (success) {
            payment.setStatus("success");
            payment.setErrorCode(null);
            payment.setErrorDescription(null);
        } else {
            payment.setStatus("failed");
            payment.setErrorCode("PMT_FAILED");
            payment.setErrorDescription("Payment authorization failed");
        }
        payment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        Merchant merchant = merchantOpt.get();
        webhookService.enqueueWebhook(merchant, success ? "payment.success" : "payment.failed", paymentService.buildPaymentPayload(payment));
    }

    private boolean determineOutcome(Payment payment) {
        if (testMode) {
            return testPaymentSuccess;
        }
        if ("upi".equalsIgnoreCase(payment.getMethod())) {
            return random.nextDouble() < 0.9;
        }
        return random.nextDouble() < 0.95;
    }
}
