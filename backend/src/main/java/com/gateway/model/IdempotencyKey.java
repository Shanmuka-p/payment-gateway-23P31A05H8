package com.gateway.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyKeyId.class)
public class IdempotencyKey {
    @Id
    @Column(name = "key", length = 255)
    private String key;

    @Id
    @Column(name = "merchant_id", columnDefinition = "uuid")
    private UUID merchantId;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String response;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
