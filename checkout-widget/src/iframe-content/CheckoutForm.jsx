import React, { useState } from 'react';

export default function CheckoutForm({ orderId, apiKey }) {
  const [vpa, setVpa] = useState('user@paytm');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const sendMessageToParent = (type, data) => {
    window.parent.postMessage({ type, data }, '*');
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // Create payment via backend API
      const response = await fetch('http://localhost:8000/api/v1/payments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Api-Key': apiKey || 'key_test_abc123',
          'X-Api-Secret': 'secret_test_xyz789'
        },
        body: JSON.stringify({
          orderId: orderId,
          method: 'upi',
          vpa: vpa
        })
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error?.description || 'Payment failed');
      }

      const paymentData = await response.json();
      
      setLoading(false);
      sendMessageToParent('payment_success', { 
        paymentId: paymentData.id, 
        orderId: orderId, 
        vpa: vpa,
        amount: paymentData.amount,
        currency: paymentData.currency,
        status: paymentData.status
      });
    } catch (err) {
      console.error('Payment error:', err);
      setLoading(false);
      setError(err.message);
      sendMessageToParent('payment_failed', { 
        error: err.message, 
        orderId: orderId 
      });
    }
  };

  return (
    <div className="pg-iframe-shell">
      <h3>Complete payment for {orderId}</h3>
      <form onSubmit={onSubmit}>
        <label>UPI VPA</label>
        <input 
          data-test-id="iframe-vpa" 
          value={vpa} 
          onChange={(e) => setVpa(e.target.value)} 
          placeholder="yourname@paytm"
          required
        />
        {error && (
          <div className="error">
            {error}
          </div>
        )}
        <div className="actions">
          <button data-test-id="iframe-pay" type="submit" disabled={loading}>
            {loading ? 'Processing payment...' : 'Pay ₹500.00'}
          </button>
          <button type="button" className="ghost" data-test-id="iframe-cancel" onClick={() => sendMessageToParent('close_modal', {})}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
