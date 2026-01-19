package com.gateway.config;

import com.gateway.model.Merchant;
import com.gateway.repo.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.OffsetDateTime;
import java.util.UUID;

@Configuration
@Profile("local")
public class LocalDataInitializer implements CommandLineRunner {

    @Autowired
    private MerchantRepository merchantRepository;

    @Override
    public void run(String... args) throws Exception {
        if (merchantRepository.findByApiKeyAndApiSecret("key_test_abc123", "secret_test_xyz789").isEmpty()) {
            Merchant testMerchant = new Merchant();
            testMerchant.setId(UUID.randomUUID());
            testMerchant.setEmail("test@example.com");
            testMerchant.setName("Test Merchant");
            testMerchant.setApiKey("key_test_abc123");
            testMerchant.setApiSecret("secret_test_xyz789");
            testMerchant.setWebhookUrl("http://localhost:4000/webhook");
            testMerchant.setWebhookSecret("whsec_test_abc123");
            testMerchant.setCreatedAt(OffsetDateTime.now());

            merchantRepository.save(testMerchant);
            System.out.println("✅ Test merchant created: key_test_abc123");
        } else {
            System.out.println("✅ Test merchant already exists");
        }
    }
}
