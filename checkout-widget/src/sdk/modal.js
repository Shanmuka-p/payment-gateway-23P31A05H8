export function createModal(iframeSrc, onClose, theme = 'light') {
  const overlay = document.createElement('div');
  overlay.id = 'payment-gateway-modal';
  overlay.setAttribute('data-test-id', 'payment-modal');
  overlay.className = 'pg-overlay';
  overlay.setAttribute('data-theme', theme);

  const inner = document.createElement('div');
  inner.className = 'pg-modal-content';

  const iframe = document.createElement('iframe');
  iframe.setAttribute('data-test-id', 'payment-iframe');
  iframe.src = iframeSrc;
  iframe.className = 'pg-iframe';
  iframe.allow = 'payment *';

  const closeBtn = document.createElement('button');
  closeBtn.setAttribute('data-test-id', 'close-modal-button');
  closeBtn.className = 'pg-close-button';
  closeBtn.innerText = '×';
  closeBtn.onclick = () => {
    onClose();
  };

  inner.appendChild(iframe);
  inner.appendChild(closeBtn);
  overlay.appendChild(inner);

  function open() {
    document.body.appendChild(overlay);
  }

  function close() {
    if (overlay.parentNode) {
      overlay.parentNode.removeChild(overlay);
    }
  }

  return { open, close, iframe };
}
