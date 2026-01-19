@echo off
echo Testing Payment Gateway API...
echo.

echo Test 1: Health Check
powershell -Command "(Invoke-WebRequest -Uri 'http://localhost:8000/actuator/health').Content"
echo.
echo.

echo Test 2: Create Payment
powershell -Command "Invoke-WebRequest -Uri 'http://localhost:8000/api/v1/payments' -Method POST -Headers @{'X-Api-Key'='key_test_abc123';'X-Api-Secret'='secret_test_xyz789';'Content-Type'='application/json'} -Body '{\"orderId\": \"order_test_001\", \"method\": \"upi\", \"vpa\": \"user@paytm\"}' | ConvertTo-Json"
echo.

echo All tests completed!
pause
