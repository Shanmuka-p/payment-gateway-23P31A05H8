import React from 'react';
import { createRoot } from 'react-dom/client';
import CheckoutForm from './CheckoutForm';
import './styles.css';

function getParam(name) {
  const params = new URLSearchParams(window.location.search);
  return params.get(name);
}

const container = document.getElementById('root');
if (container) {
  const initialTheme = getParam('theme') || (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', initialTheme);
  const root = createRoot(container);
  root.render(
    <CheckoutForm 
      orderId={getParam('order_id') || 'order_demo'} 
      apiKey={getParam('key') || 'key_test_abc123'} 
    />
  );
}
