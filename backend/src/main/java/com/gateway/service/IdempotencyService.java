package com.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.model.IdempotencyKey;
import com.gateway.model.IdempotencyKeyId;
import com.gateway.repo.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {
    private final IdempotencyKeyRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    public Optional<JsonNode> findCached(UUID merchantId, String key) {
        Optional<IdempotencyKey> stored = repository.findById(new IdempotencyKeyId(key, merchantId));
        if (stored.isEmpty()) return Optional.empty();
        IdempotencyKey entity = stored.get();
        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            repository.delete(entity);
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readTree(entity.getResponse()));
        } catch (Exception e) {
            repository.delete(entity);
            return Optional.empty();
        }
    }

    public void save(UUID merchantId, String key, Object response) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = now.plus(24, ChronoUnit.HOURS);
        try {
            String json = mapper.writeValueAsString(response);
            IdempotencyKey entity = new IdempotencyKey();
            entity.setKey(key);
            entity.setMerchantId(merchantId);
            entity.setResponse(json);
            entity.setCreatedAt(now);
            entity.setExpiresAt(expires);
            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store idempotency response", e);
        }
    }
}
