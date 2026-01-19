# Payment Gateway with Async Jobs and Webhooks

A production-ready payment gateway system built with **Spring Boot**, **React**, **PostgreSQL**, and **Redis**. This project demonstrates advanced architectural patterns including asynchronous job processing, webhook delivery with retry mechanisms, embeddable JavaScript SDK, and comprehensive refund management.

## 🚀 Features

### Core Capabilities
- **Asynchronous Payment Processing** - Redis-backed job queues with background workers
- **Webhook System** - Event delivery with HMAC signature verification and exponential backoff retry (5 attempts)
- **Embeddable JavaScript SDK** - Cross-origin modal/iframe integration for merchants
- **Refund Management** - Full and partial refunds processed asynchronously
- **Idempotency Keys** - Prevent duplicate charges on network retries (24-hour expiry)
- **Enhanced Dashboard** - Webhook configuration, delivery logs, manual retry, and integration docs

### Technical Highlights
- **Event-Driven Architecture** - Webhooks for `payment.created`, `payment.success`, `payment.failed`, `refund.processed`
- **Job Workers** - Separate worker processes for payment processing, webhook delivery, and refunds
- **Test Mode Support** - Configurable delays and deterministic outcomes for automated testing
- **Docker Compose** - Full stack deployment with Postgres, Redis, API, worker, checkout widget, and dashboard

## 📋 Prerequisites

- **Docker** and **Docker Compose** (recommended)
- **Java 17+** and **Maven 3.9+** (for local development)
- **Node.js 20+** (for local frontend development)
- **PostgreSQL 15+** (if running without Docker)
- **Redis 7+** (if running without Docker)

## 🏗️ Architecture

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   Dashboard     │       │  Checkout SDK   │       │  Merchant Site  │
│   (React)       │       │  (Webpack UMD)  │       │                 │
└────────┬────────┘       └────────┬────────┘       └────────┬────────┘
         │                         │                         │
         └─────────────────────────┼─────────────────────────┘
                                   │
                         ┌─────────▼─────────┐
                         │   API Gateway     │
                         │  (Spring Boot)    │
                         └─────────┬─────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
         ┌──────────▼────────┐    │    ┌─────────▼────────┐
         │   PostgreSQL      │    │    │     Redis        │
         │   (Persistent)    │    │    │  (Job Queue)     │
         └───────────────────┘    │    └─────────┬────────┘
                                  │              │
                         ┌────────▼──────────┐   │
                         │  Worker Service   │◄──┘
                         │  (Spring Boot)    │
                         └───────────────────┘
```

## 🛠️ Quick Start

### Using Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/Shanmuka-p/payment-gateway-23P31A05H8.git
cd payment-gateway-23P31A05H8

# Start all services
docker-compose up --build

# Services will be available at:
# API:       http://localhost:8000
# Dashboard: http://localhost:3000
# Checkout:  http://localhost:3001
```

### Manual Setup

#### 1. Start PostgreSQL and Redis

```bash
# PostgreSQL
docker run -d --name postgres_gateway \
  -e POSTGRES_USER=gateway_user \
  -e POSTGRES_PASSWORD=vinay2122@ \
  -e POSTGRES_DB=payment_gateway \
  -p 5432:5432 postgres:15-alpine

# Redis
docker run -d --name redis_gateway \
  -p 6379:6379 redis:7-alpine
```

#### 2. Build and Run Backend API

```bash
cd backend
mvn clean package -DskipTests
java -jar target/payment-gateway-0.1.0.jar
```

#### 3. Build and Run Worker

```bash
cd backend
java -jar target/payment-gateway-0.1.0.jar \
  --spring.profiles.active=worker
```

#### 4. Build and Run Checkout Widget

```bash
cd checkout-widget
npm install
npm run build
npm run serve
```

#### 5. Build and Run Dashboard

```bash
cd dashboard
npm install
npm run build
npm run preview
```

## 🔑 API Documentation

### Authentication

All API endpoints require authentication via headers:

```bash
X-Api-Key: key_test_abc123
X-Api-Secret: secret_test_xyz789
```

### Create Payment

```bash
POST /api/v1/payments
Content-Type: application/json
Idempotency-Key: unique_request_id_123  # Optional

{
  "orderId": "order_NXhj67fGH2jk9mPq",
  "method": "upi",
  "vpa": "user@paytm"
}

# Response 201
{
  "id": "pay_H8sK3jD9s2L1pQr",
  "order_id": "order_NXhj67fGH2jk9mPq",
  "amount": 50000,
  "currency": "INR",
  "method": "upi",
  "vpa": "user@paytm",
  "status": "pending",
  "created_at": "2024-01-15T10:31:00Z"
}
```

### Capture Payment

```bash
POST /api/v1/payments/{payment_id}/capture
Content-Type: application/json

{
  "amount": 50000
}

# Response 200
{
  "id": "pay_H8sK3jD9s2L1pQr",
  "status": "success",
  "captured": true,
  "updated_at": "2024-01-15T10:32:00Z"
}
```

### Create Refund

```bash
POST /api/v1/payments/{payment_id}/refunds
Content-Type: application/json

{
  "amount": 50000,
  "reason": "Customer requested refund"
}

# Response 201
{
  "id": "rfnd_K9pL2mN4oQ5r",
  "payment_id": "pay_H8sK3jD9s2L1pQr",
  "amount": 50000,
  "reason": "Customer requested refund",
  "status": "pending",
  "created_at": "2024-01-15T10:33:00Z"
}
```

### List Webhook Logs

```bash
GET /api/v1/webhooks?limit=10&offset=0

# Response 200
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "event": "payment.success",
      "status": "success",
      "attempts": 1,
      "created_at": "2024-01-15T10:31:10Z",
      "last_attempt_at": "2024-01-15T10:31:11Z",
      "response_code": 200
    }
  ],
  "total": 1,
  "limit": 10,
  "offset": 0
}
```

### Retry Webhook

```bash
POST /api/v1/webhooks/{webhook_id}/retry

# Response 200
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "status": "pending",
  "message": "Webhook retry scheduled"
}
```

### Job Queue Status (Test Endpoint)

```bash
GET /api/v1/test/jobs/status

# Response 200
{
  "pending": 5,
  "processing": 2,
  "completed": 100,
  "failed": 0,
  "worker_status": "running"
}
```

## 🔌 Webhook Integration

### Webhook Events

- `payment.created` - Payment record created
- `payment.pending` - Payment entered pending state
- `payment.success` - Payment succeeded
- `payment.failed` - Payment failed
- `refund.created` - Refund initiated
- `refund.processed` - Refund completed

### Webhook Payload Format

```json
{
  "event": "payment.success",
  "timestamp": 1705315870,
  "data": {
    "payment": {
      "id": "pay_H8sK3jD9s2L1pQr",
      "order_id": "order_NXhj67fGH2jk9mPq",
      "amount": 50000,
      "currency": "INR",
      "method": "upi",
      "status": "success",
      "created_at": "2024-01-15T10:31:00Z"
    }
  }
}
```

### Signature Verification

Webhooks are signed with HMAC-SHA256. Verify the signature:

```javascript
const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');
  
  return signature === expectedSignature;
}

// Express example
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const payload = JSON.stringify(req.body);
  
  if (!verifyWebhook(req.body, signature, 'whsec_test_abc123')) {
    return res.status(401).send('Invalid signature');
  }
  
  console.log('Webhook verified:', req.body.event);
  res.status(200).send('OK');
});
```

### Retry Logic

Webhooks are automatically retried with exponential backoff:

- **Attempt 1**: Immediate (0 seconds)
- **Attempt 2**: After 1 minute (60 seconds)
- **Attempt 3**: After 5 minutes (300 seconds)
- **Attempt 4**: After 30 minutes (1800 seconds)
- **Attempt 5**: After 2 hours (7200 seconds)

After 5 failed attempts, webhooks are marked permanently failed. Use manual retry from the dashboard if needed.

## 💳 Embeddable SDK Integration

### Include SDK

```html
<script src="http://localhost:3001/checkout.js"></script>
```

### Usage Example

```html
<button id="pay-button">Pay Now</button>

<script>
document.getElementById('pay-button').addEventListener('click', function() {
  const checkout = new PaymentGateway({
    key: 'key_test_abc123',
    orderId: 'order_xyz',
    onSuccess: function(response) {
      console.log('Payment successful:', response.paymentId);
      // Redirect to success page
    },
    onFailure: function(error) {
      console.log('Payment failed:', error);
      // Show error message
    },
    onClose: function() {
      console.log('Modal closed');
    }
  });
  
  checkout.open();
});
</script>
```

### SDK Configuration

| Option | Type | Required | Description |
|--------|------|----------|-------------|
| `key` | String | Yes | Merchant API key |
| `orderId` | String | Yes | Order identifier |
| `onSuccess` | Function | No | Callback when payment succeeds |
| `onFailure` | Function | No | Callback when payment fails |
| `onClose` | Function | No | Callback when modal is closed |
| `host` | String | No | Checkout server URL (default: `http://localhost:3001`) |

## 🧪 Testing

### Test Credentials

```
API Key:    key_test_abc123
API Secret: secret_test_xyz789
Webhook:    whsec_test_abc123
```

### Test Mode Configuration

Enable deterministic behavior for automated testing:

```yaml
# backend/src/main/resources/application.yml
app:
  test-mode: true
  test-processing-delay: 1000  # milliseconds
  test-payment-success: true
  webhook-retry-intervals-test: true  # Uses 5s, 10s, 15s, 20s intervals
```

### Test Webhook Receiver

```javascript
// test-merchant/webhook-receiver.js
const express = require('express');
const crypto = require('crypto');

const app = express();
app.use(express.json());

app.post('/webhook', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const payload = JSON.stringify(req.body);
  
  const expectedSignature = crypto
    .createHmac('sha256', 'whsec_test_abc123')
    .update(payload)
    .digest('hex');
  
  if (signature !== expectedSignature) {
    console.log('[invalid] Webhook signature mismatch');
    return res.status(401).send('Invalid signature');
  }
  
  console.log('[ok] Webhook verified:', req.body.event);
  console.log('Payment ID:', req.body.data.payment.id);
  
  res.status(200).send('OK');
});

app.listen(4000, () => {
  console.log('Test merchant webhook running on port 4000');
});
```

Run the test receiver and configure webhook URL to:
- **Mac/Windows**: `http://host.docker.internal:4000/webhook`
- **Linux**: `http://172.17.0.1:4000/webhook`

## Database Schema

### Merchants Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `name` | VARCHAR(100) | Merchant name |
| `email` | VARCHAR(255) | Unique email |
| `api_key` | VARCHAR(64) | API authentication key |
| `api_secret` | VARCHAR(64) | API secret |
| `webhook_url` | TEXT | Webhook endpoint URL |
| `webhook_secret` | VARCHAR(64) | HMAC secret for signatures |
| `created_at` | TIMESTAMPTZ | Creation timestamp |

### Payments Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(64) | Primary key (format: `pay_` + 16 chars) |
| `merchant_id` | UUID | Foreign key to merchants |
| `order_id` | VARCHAR(64) | Merchant order ID |
| `amount` | BIGINT | Amount in smallest currency unit |
| `currency` | VARCHAR(10) | Currency code (INR) |
| `method` | VARCHAR(20) | Payment method (upi, card) |
| `vpa` | VARCHAR(255) | UPI VPA |
| `card_last4` | VARCHAR(4) | Last 4 digits of card |
| `status` | VARCHAR(20) | pending, success, failed |
| `error_code` | VARCHAR(50) | Error code if failed |
| `error_description` | TEXT | Error description |
| `captured` | BOOLEAN | Whether payment captured |
| `created_at` | TIMESTAMPTZ | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | Last update timestamp |

### Refunds Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(64) | Primary key (format: `rfnd_` + 16 chars) |
| `payment_id` | VARCHAR(64) | Foreign key to payments |
| `merchant_id` | UUID | Foreign key to merchants |
| `amount` | BIGINT | Refund amount |
| `reason` | TEXT | Refund reason |
| `status` | VARCHAR(20) | pending, processed |
| `created_at` | TIMESTAMPTZ | Creation timestamp |
| `processed_at` | TIMESTAMPTZ | Processing timestamp |

### Webhook Logs Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `merchant_id` | UUID | Foreign key to merchants |
| `event` | VARCHAR(50) | Event type |
| `payload` | JSONB | Event payload |
| `status` | VARCHAR(20) | pending, success, failed |
| `attempts` | INT | Delivery attempt count |
| `last_attempt_at` | TIMESTAMPTZ | Last attempt timestamp |
| `next_retry_at` | TIMESTAMPTZ | Next retry timestamp |
| `response_code` | INT | HTTP response code |
| `response_body` | TEXT | Response body |
| `created_at` | TIMESTAMPTZ | Creation timestamp |

### Idempotency Keys Table

| Column | Type | Description |
|--------|------|-------------|
| `key` | VARCHAR(255) | Idempotency key (part of composite PK) |
| `merchant_id` | UUID | Merchant ID (part of composite PK) |
| `response` | JSONB | Cached API response |
| `created_at` | TIMESTAMPTZ | Creation timestamp |
| `expires_at` | TIMESTAMPTZ | Expiration timestamp (24 hours) |

## Configuration

### Environment Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/payment_gateway
SPRING_DATASOURCE_USERNAME=gateway_user
SPRING_DATASOURCE_PASSWORD=vinay2122@

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Test Mode
TEST_MODE=false
TEST_PROCESSING_DELAY=1000
TEST_PAYMENT_SUCCESS=true
WEBHOOK_RETRY_INTERVALS_TEST=false

# Worker Profile
SPRING_PROFILES_ACTIVE=worker  # For worker service only
```

## Docker Services

| Service | Container | Port | Description |
|---------|-----------|------|-------------|
| Postgres | `postgres_gateway` | 5432 | PostgreSQL database |
| Redis | `redis_gateway` | 6379 | Job queue and cache |
| API | `gateway_api` | 8000 | Spring Boot REST API |
| Worker | `gateway_worker` | - | Background job processor |
| Checkout | `gateway_checkout` | 3001 | Embeddable SDK server |
| Dashboard | `gateway_dashboard` | 3000 | React admin dashboard |

## Design Patterns

- **Repository Pattern** - Data access abstraction with JPA repositories
- **Service Layer** - Business logic separation from controllers
- **Queue-Based Processing** - Asynchronous job execution with Redis
- **Event-Driven Architecture** - Webhook delivery for state changes
- **Idempotency** - Prevent duplicate operations with key-based caching
- **Retry with Exponential Backoff** - Resilient webhook delivery
- **HMAC Signature Verification** - Secure webhook authentication

## Security Considerations

- **API Authentication** - Key/secret pair validation on every request
- **Webhook Signatures** - HMAC-SHA256 to prevent tampering
- **Idempotency Keys** - 24-hour expiration to prevent replay attacks
- **Input Validation** - Jakarta Bean Validation on all DTOs
- **SQL Injection Prevention** - Parameterized queries via JPA
- **CORS** - Configure allowed origins for production deployment

## Monitoring

### Health Check

```bash
GET /actuator/health

# Response
{
  "status": "UP"
}
```

### Job Queue Metrics

Monitor queue status via test endpoint:

```bash
GET /api/v1/test/jobs/status
```

### Logs

- **API Logs**: Spring Boot structured logging
- **Worker Logs**: Payment/refund/webhook processing events
- **Webhook Delivery**: Detailed attempt logs in `webhook_logs` table

## 🚨 Troubleshooting

### Workers Not Processing Jobs

```bash
# Check worker heartbeat
redis-cli GET worker:status

# Verify queue contents
redis-cli LLEN queue:payment.process
redis-cli LLEN queue:webhook.deliver
redis-cli LLEN queue:refund.process

# Restart worker
docker-compose restart worker
```

### Webhooks Not Delivering

1. Verify `webhook_url` is set in merchants table
2. Check `webhook_logs` for error details
3. Ensure webhook endpoint is accessible from Docker network
4. Validate signature verification in merchant endpoint

### Database Connection Issues

```bash
# Test Postgres connection
docker exec -it postgres_gateway psql -U gateway_user -d payment_gateway

# Check migration status
docker logs gateway_api | grep Flyway
```

## 📚 Tech Stack

**Backend**
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Data Redis
- PostgreSQL 15
- Redis 7
- Flyway (migrations)
- Jackson (JSON)

**Frontend**
- React 18
- Vite 5
- Axios
- Webpack 5 (SDK bundling)

**Infrastructure**
- Docker & Docker Compose
- Maven 3.9
- Node.js 20

## 🤝 Contributing

This is a learning project demonstrating production-ready payment gateway patterns. Feel free to fork and experiment!

## 📄 License

MIT License - see LICENSE file for details

## 🙏 Acknowledgments

Built to demonstrate advanced patterns used by companies like Stripe, Razorpay, and PayPal. This project showcases:
- Asynchronous job processing
- Event-driven webhook systems
- Embeddable payment widgets
- Idempotent API design
- Resilient retry mechanisms

Perfect for understanding how production payment systems handle scale, reliability, and security challenges.

---

**Built with ❤️ for learning advanced backend architecture**
