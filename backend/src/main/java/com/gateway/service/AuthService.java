package com.gateway.service;

import com.gateway.model.Merchant;
import com.gateway.repo.MerchantRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final MerchantRepository merchantRepository;

    public AuthService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public Optional<Merchant> authenticate(String apiKey, String apiSecret) {
        if (apiKey == null || apiSecret == null) return Optional.empty();
        return merchantRepository.findByApiKeyAndApiSecret(apiKey, apiSecret);
    }
}
