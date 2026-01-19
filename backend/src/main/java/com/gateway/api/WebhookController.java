package com.gateway.api;

import com.gateway.dto.ErrorResponse;
import com.gateway.model.Merchant;
import com.gateway.model.WebhookLog;
import com.gateway.service.AuthService;
import com.gateway.service.WebhookService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    private final AuthService authService;
    private final WebhookService webhookService;

    public WebhookController(AuthService authService, WebhookService webhookService) {
        this.authService = authService;
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                  @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
                                  @RequestParam(defaultValue = "10") int limit,
                                  @RequestParam(defaultValue = "0") int offset) {
        Optional<Merchant> merchantOpt = authService.authenticate(apiKey, apiSecret);
        if (merchantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Invalid API credentials"));
        }
        Merchant merchant = merchantOpt.get();
        Page<WebhookLog> page = webhookService.listForMerchant(merchant.getId(), offset, limit);
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", page.map(log -> mapLog(log)).getContent());
        resp.put("total", page.getTotalElements());
        resp.put("limit", limit);
        resp.put("offset", offset);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{webhookId}/retry")
    public ResponseEntity<?> retry(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                   @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
                                   @PathVariable String webhookId) {
        Optional<Merchant> merchantOpt = authService.authenticate(apiKey, apiSecret);
        if (merchantOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Invalid API credentials"));
        }
        Merchant merchant = merchantOpt.get();
        Optional<WebhookLog> logOpt = webhookService.resetAndEnqueue(UUID.fromString(webhookId), merchant.getId());
        if (logOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Webhook not found"));
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", webhookId);
        resp.put("status", "pending");
        resp.put("message", "Webhook retry scheduled");
        return ResponseEntity.ok(resp);
    }

    private Map<String, Object> mapLog(WebhookLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("event", log.getEvent());
        map.put("status", log.getStatus());
        map.put("attempts", log.getAttempts());
        map.put("created_at", log.getCreatedAt());
        map.put("last_attempt_at", log.getLastAttemptAt());
        map.put("response_code", log.getResponseCode());
        return map;
    }
}
