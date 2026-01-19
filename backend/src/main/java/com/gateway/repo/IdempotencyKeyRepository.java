package com.gateway.repo;

import com.gateway.model.IdempotencyKey;
import com.gateway.model.IdempotencyKeyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {
}
