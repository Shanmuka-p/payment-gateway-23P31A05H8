import React, { useEffect, useState } from 'react';

function Nav({ active, setActive }) {
  return (
    <nav className="nav">
      <button className={active === 'webhooks' ? 'active' : ''} onClick={() => setActive('webhooks')}>
        Webhooks
      </button>
      <button className={active === 'docs' ? 'active' : ''} onClick={() => setActive('docs')}>
        API Docs
      </button>
    </nav>
  );
}
const RefreshIcon = ({ size = 16 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M4 4v6h6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M20 20v-6h-6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M5 14a7 7 0 0 0 12.9 2" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M19 10A7 7 0 0 0 6.1 8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const SunIcon = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="12" cy="12" r="5" stroke="currentColor" strokeWidth="1.6" />
    <path d="M12 3v2M12 19v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M3 12h2M19 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
  </svg>
);

const MoonIcon = ({ size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M21 12.79A9 9 0 0 1 11.21 3 7 7 0 1 0 21 12.79Z" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

function WebhookConfig() {
  const [url, setUrl] = useState('http://localhost:4000/webhook');
  const [secret, setSecret] = useState('whsec_test_abc123');
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch real webhook logs from backend
  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch('http://localhost:8000/api/v1/webhooks?limit=50', {
        headers: {
          'X-Api-Key': 'key_test_abc123',
          'X-Api-Secret': 'secret_test_xyz789'
        }
      });
      
      if (!response.ok) {
        throw new Error(`Failed to fetch logs: ${response.status}`);
      }
      
      const data = await response.json();
      setLogs(data.data || []);
    } catch (err) {
      console.error('Error fetching webhook logs:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRetry = async (webhookId) => {
    try {
      const response = await fetch(`http://localhost:8000/api/v1/webhooks/${webhookId}/retry`, {
        method: 'POST',
        headers: {
          'X-Api-Key': 'key_test_abc123',
          'X-Api-Secret': 'secret_test_xyz789'
        }
      });
      
      if (response.ok) {
        alert('Webhook retry scheduled!');
        fetchLogs(); // Refresh the list
      } else {
        alert('Failed to retry webhook');
      }
    } catch (err) {
      console.error('Error retrying webhook:', err);
      alert('Error: ' + err.message);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'Never';
    return new Date(dateStr).toLocaleString();
  };

  return (
    <section data-test-id="webhook-config" className="card">
      <h2>Webhook Configuration</h2>
      <form data-test-id="webhook-config-form" className="grid">
        <label>
          Webhook URL
          <input
            data-test-id="webhook-url-input"
            type="url"
            value={url}
            placeholder="http://localhost:4000/webhook"
            onChange={(e) => setUrl(e.target.value)}
          />
        </label>
        <div className="secret-row">
          <div>
            <div>Webhook Secret</div>
            <span data-test-id="webhook-secret" className="pill">{secret}</span>
          </div>
          <button data-test-id="regenerate-secret-button" type="button" className="ghost" onClick={() => setSecret(`whsec_${Date.now()}`)}>
            Regenerate
          </button>
        </div>
        <div className="actions">
          <button data-test-id="save-webhook-button" type="submit" onClick={(e) => { e.preventDefault(); alert('Configuration saved! (Demo)'); }}>
            Save Configuration
          </button>
          <button data-test-id="test-webhook-button" type="button" className="ghost" onClick={() => alert('Test webhook sent! (Demo)')}>
            Send Test Webhook
          </button>
        </div>
      </form>

      <h3 className="section-heading">
        Webhook Logs 
        <button onClick={fetchLogs} className="ghost inline-btn" aria-label="Refresh webhook logs">
          <RefreshIcon size={14} />
          <span>Refresh</span>
        </button>
      </h3>

      {loading && <p>Loading webhook logs...</p>}
      {error && <p className="error-text">Error: {error}. Make sure backend is running on port 8000.</p>}
      {!loading && !error && logs.length === 0 && (
        <p className="muted">No webhook logs yet. Make a payment on <a href="http://localhost:3001/demo.html" target="_blank" rel="noreferrer">demo page</a> to see logs here.</p>
      )}
      
      {logs.length > 0 && (
        <table data-test-id="webhook-logs-table">
          <thead>
            <tr>
              <th>Event</th>
              <th>Status</th>
              <th>Attempts</th>
              <th>Last Attempt</th>
              <th>Response Code</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} data-test-id="webhook-log-item" data-webhook-id={log.id}>
                <td data-test-id="webhook-event">{log.event}</td>
                <td data-test-id="webhook-status">
                  <span className={`status-chip ${log.status}`}>
                    {log.status}
                  </span>
                </td>
                <td data-test-id="webhook-attempts">{log.attempts}</td>
                <td data-test-id="webhook-last-attempt">{formatDate(log.last_attempt_at)}</td>
                <td data-test-id="webhook-response-code">{log.response_code || 'N/A'}</td>
                <td>
                  <button 
                    data-test-id="retry-webhook-button" 
                    data-webhook-id={log.id} 
                    className="ghost"
                    onClick={() => handleRetry(log.id)}
                    disabled={log.status === 'success'}
                  >
                    Retry
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function ApiDocs() {
  return (
    <section data-test-id="api-docs" className="card">
      <h2>Integration Guide</h2>
      <section data-test-id="section-create-order">
        <h3>1. Create Order</h3>
        <pre data-test-id="code-snippet-create-order"><code>{`curl -X POST http://localhost:8000/api/v1/orders \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "currency": "INR",
    "receipt": "receipt_123"
  }'
`}</code></pre>
      </section>

      <section data-test-id="section-sdk-integration">
        <h3>2. SDK Integration</h3>
        <pre data-test-id="code-snippet-sdk"><code>{`<script src="http://localhost:3001/checkout.js"></script>
<script>
const checkout = new PaymentGateway({
  key: 'key_test_abc123',
  orderId: 'order_xyz',
  onSuccess: (response) => {
    console.log('Payment ID:', response.paymentId);
  }
});
checkout.open();
</script>
`}</code></pre>
      </section>

      <section data-test-id="section-webhook-verification">
        <h3>3. Verify Webhook Signature</h3>
        <pre data-test-id="code-snippet-webhook"><code>{`const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');

  return signature === expectedSignature;
}
`}</code></pre>
      </section>
    </section>
  );
}

export default function App() {
  const [active, setActive] = useState('webhooks');
  const [theme, setTheme] = useState('dark');

  useEffect(() => {
    const saved = localStorage.getItem('pg-theme');
    if (saved) {
      setTheme(saved);
      document.documentElement.setAttribute('data-theme', saved);
    }
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('pg-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((t) => (t === 'dark' ? 'light' : 'dark'));
  };

  return (
    <div className="shell">
      <header className="hero">
        <div>
          <p className="eyebrow">Payment Gateway</p>
          <h1>Async jobs, webhooks, and SDK hub</h1>
          <p className="muted">Configure webhooks, monitor deliveries, and grab integration snippets in one place.</p>
        </div>
        <div className="hero-actions">
          <div className="pill">Sandbox</div>
          <button className="ghost theme-toggle" onClick={toggleTheme} aria-label="Toggle theme">
            {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
            <span>{theme === 'dark' ? 'Light' : 'Dark'} mode</span>
          </button>
        </div>
      </header>
      <Nav active={active} setActive={setActive} />
      {active === 'webhooks' ? <WebhookConfig /> : <ApiDocs />}
    </div>
  );
}
