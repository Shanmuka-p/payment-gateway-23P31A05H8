#!/bin/bash
echo "Testing Payment Gateway API..."
echo ""

echo "✓ Test 1: Health Check"
curl -X GET http://localhost:8000/actuator/health
echo ""
echo ""

echo "✓ Test 2: Create Payment"
curl -X POST http://localhost:8000/api/v1/payments \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order_test_001",
    "method": "upi",
    "vpa": "user@paytm"
  }' | jq '.'

echo ""
echo "✓ All tests completed!"
