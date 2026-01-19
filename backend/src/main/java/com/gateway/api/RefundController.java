package com.gateway.api;

import com.gateway.dto.CreateRefundRequest;
import com.gateway.dto.ErrorResponse;
import com.gateway.model.Merchant;
import com.gateway.model.Refund;
import com.gateway.repo.RefundRepository;
import com.gateway.service.AuthService;
import com.gateway.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class RefundController {
    private final AuthService authService;
    private final RefundService refundService;
    private final RefundRepository refundRepository;

    public RefundController(AuthService authService, RefundService refundService, RefundRepository refundRepository) {
        this.authService = authService;
        this.refundService = refundService;
        this.refundRepository = refundRepository;
    }

    @PostMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<?> createRefund(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                          @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
                                          @PathVariable String paymentId,
                                          @Valid @RequestBody CreateRefundRequest request) {
        Optional<Merchant> merchantOpt = authService.authenticate(apiKey, apiSecret);
        if (merchantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Invalid API credentials"));
        }
        Merchant merchant = merchantOpt.get();
        Map<String, Object> response = refundService.createRefund(merchant, paymentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<?> getRefund(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                       @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
                                       @PathVariable String refundId) {
        Optional<Merchant> merchantOpt = authService.authenticate(apiKey, apiSecret);
        if (merchantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Invalid API credentials"));
        }
        Merchant merchant = merchantOpt.get();
        Refund refund = refundRepository.findById(refundId).orElseThrow(() -> new IllegalArgumentException("Refund not found"));
        if (!refund.getMerchantId().equals(merchant.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Refund not found"));
        }
        return ResponseEntity.ok(refundService.refundPayload(refund));
    }
}
