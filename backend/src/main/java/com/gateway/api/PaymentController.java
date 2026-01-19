package com.gateway.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.gateway.dto.CapturePaymentRequest;
import com.gateway.dto.CreatePaymentRequest;
import com.gateway.dto.ErrorResponse;
import com.gateway.model.Merchant;
import com.gateway.service.AuthService;
import com.gateway.service.IdempotencyService;
import com.gateway.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final AuthService authService;
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public PaymentController(AuthService authService, PaymentService paymentService, IdempotencyService idempotencyService) {
        this.authService = authService;
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                           @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
                                           @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                                           @Valid @RequestBody CreatePaymentRequest request) {
        Optional<Merchant> merchantOpt = authService.authenticate(apiKey, apiSecret);
        if (merchantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Invalid API credentials"));
        }
        Merchant merchant = merchantOpt.get();

        if (idemKey != null) {
            Optional<JsonNode> cached = idempotencyService.findCached(merchant.getId(), idemKey);
            if (cached.isPresent()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(cached.get());
            }
        }

        Map<String, Object> response = paymentService.createPayment(merchant, request);
        if (idemKey != null) {
            idempotencyService.save(merchant.getId(), idemKey, response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<?> capturePayment(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                            @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
                                            @PathVariable String paymentId,
                                            @Valid @RequestBody CapturePaymentRequest request) {
        Optional<Merchant> merchantOpt = authService.authenticate(apiKey, apiSecret);
        if (merchantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Invalid API credentials"));
        }
        Merchant merchant = merchantOpt.get();
        Map<String, Object> payload = paymentService.capturePayment(merchant, paymentId, request);
        return ResponseEntity.ok(payload);
    }
}
