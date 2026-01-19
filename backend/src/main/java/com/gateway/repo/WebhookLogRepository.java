package com.gateway.repo;

import com.gateway.model.WebhookLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface WebhookLogRepository extends JpaRepository<WebhookLog, java.util.UUID> {
    Page<WebhookLog> findByMerchantId(UUID merchantId, Pageable pageable);
    List<WebhookLog> findByStatusAndNextRetryAtLessThanEqual(String status, OffsetDateTime time);
}
