import PaymentGateway from './sdk/PaymentGateway';
export default PaymentGateway;

// expose globally for UMD consumers
if (typeof window !== 'undefined') {
  window.PaymentGateway = PaymentGateway;
}
