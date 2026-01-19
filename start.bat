@echo off
echo Starting all services...
echo.

echo Starting Docker Compose...
docker-compose up -d

echo.
echo Waiting for services to be healthy...
timeout /t 10 /nobreak > nul

echo.
echo Service Status:
docker-compose ps

echo.
echo All services started!
echo.
echo Access URLs:
echo   - API:       http://localhost:8000
echo   - Dashboard: http://localhost:3000
echo   - Checkout:  http://localhost:3001
echo   - Demo:      http://localhost:3001/demo.html
echo.
echo Test Credentials:
echo   - API Key:    key_test_abc123
echo   - API Secret: secret_test_xyz789
echo.
echo Check logs:
echo   docker-compose logs -f api
echo   docker-compose logs -f worker
