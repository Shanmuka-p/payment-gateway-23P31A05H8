INSERT INTO merchants (id, name, email, api_key, api_secret, webhook_url, webhook_secret, created_at)
VALUES (
    gen_random_uuid(),
    'Test Merchant',
    'test@example.com',
    'key_test_abc123',
    'secret_test_xyz789',
    'http://gateway_test_merchant:4000/webhook',
    'whsec_test_abc123',
    NOW()
)
ON CONFLICT (email) DO UPDATE SET webhook_url = 'http://gateway_test_merchant:4000/webhook', webhook_secret = 'whsec_test_abc123';
