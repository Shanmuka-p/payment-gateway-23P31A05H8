import { createModal } from './modal';
import './styles.css';

class PaymentGateway {
  constructor(options = {}) {
    this.key = options.key;
    this.orderId = options.orderId;
    this.onSuccess = options.onSuccess || (() => {});
    this.onFailure = options.onFailure || (() => {});
    this.onClose = options.onClose || (() => {});
    this.host = options.host || 'http://localhost:3001';
    this.theme = options.theme || this._detectTheme();
    if (!this.key || !this.orderId) {
      throw new Error('PaymentGateway: key and orderId are required');
    }
    this.modal = null;
    this._messageHandler = this._handleMessage.bind(this);
  }

  open() {
    const iframeUrl = `${this.host}/checkout?order_id=${encodeURIComponent(this.orderId)}&embedded=true&key=${encodeURIComponent(this.key)}&theme=${encodeURIComponent(this.theme)}`;
    this.modal = createModal(iframeUrl, () => this.close(), this.theme);
    this.modal.open();
    window.addEventListener('message', this._messageHandler);
  }

  close() {
    if (this.modal) {
      this.modal.close();
      this.modal = null;
    }
    window.removeEventListener('message', this._messageHandler);
    this.onClose();
  }

  _handleMessage(event) {
    const { data } = event;
    if (!data || !data.type) return;
    if (data.type === 'payment_success') {
      this.onSuccess(data.data);
      this.close();
    } else if (data.type === 'payment_failed') {
      this.onFailure(data.data);
    } else if (data.type === 'close_modal') {
      this.close();
    }
  }

  _detectTheme() {
    const attr = document.documentElement.getAttribute('data-theme');
    if (attr === 'light' || attr === 'dark') return attr;
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }
}

export default PaymentGateway;
if (typeof window !== 'undefined') {
  window.PaymentGateway = PaymentGateway;
}
