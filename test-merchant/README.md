# Test Merchant Webhook Receiver

Simple Express server to test webhook delivery from the payment gateway.

## Usage

```bash
npm install
npm start
```

Server will run on `http://localhost:4000`

Configure your webhook URL in the dashboard:
- **Mac/Windows Docker**: `http://host.docker.internal:4000/webhook`
- **Linux Docker**: `http://172.17.0.1:4000/webhook`
- **Local**: `http://localhost:4000/webhook`

The receiver will verify HMAC signatures and log webhook events.
