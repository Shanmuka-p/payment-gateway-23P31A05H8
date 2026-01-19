package com.gateway.repo;

import com.gateway.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByApiKeyAndApiSecret(String apiKey, String apiSecret);
    Optional<Merchant> findByEmail(String email);
}
