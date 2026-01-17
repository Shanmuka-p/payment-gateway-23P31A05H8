(function(window) {
    class PaymentGateway {
        constructor(options) {
            this.options = options;
            this.validate();
        }

        validate() {
            if (!this.options.key) console.error("PaymentGateway: 'key' is required");
            if (!this.options.orderId) console.error("PaymentGateway: 'orderId' is required");
        }

        open() {            
            this.modal = document.createElement('div');
            this.modal.id = 'payment-gateway-modal';
            this.modal.setAttribute('data-test-id', 'payment-modal');
            Object.assign(this.modal.style, {
                position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
                backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex',
                justifyContent: 'center', alignItems: 'center'
            });
            const content = document.createElement('div');
            content.className = 'modal-content';
            Object.assign(content.style, {
                backgroundColor: 'white', padding: '20px', borderRadius: '8px',
                width: '400px', height: '500px', position: 'relative'
            });
            const closeBtn = document.createElement('button');
            closeBtn.innerText = '×';
            closeBtn.className = 'close-button';
            closeBtn.setAttribute('data-test-id', 'close-modal-button');
            Object.assign(closeBtn.style, {
                position: 'absolute', top: '10px', right: '10px', border: 'none',
                background: 'none', fontSize: '24px', cursor: 'pointer'
            });
            closeBtn.onclick = () => this.close();
            const iframe = document.createElement('iframe');
            iframe.setAttribute('data-test-id', 'payment-iframe');           
            iframe.src = `http://localhost:3001/checkout.html?order_id=${this.options.orderId}&key=${this.options.key}`;
            Object.assign(iframe.style, { width: '100%', height: '100%', border: 'none' });

            content.appendChild(closeBtn);
            content.appendChild(iframe);
            this.modal.appendChild(content);
            document.body.appendChild(this.modal);
           
            window.addEventListener('message', this.handleMessage.bind(this));
        }

        close() {
            if (this.modal) {
                document.body.removeChild(this.modal);
                this.modal = null;
                if (this.options.onClose) this.options.onClose();
            }
        }

        handleMessage(event) {
            if (event.data.type === 'payment_success') {
                if (this.options.onSuccess) this.options.onSuccess(event.data.data);
                this.close();
            } else if (event.data.type === 'payment_failed') {
                if (this.options.onFailure) this.options.onFailure(event.data.data);
            }
        }
    }

    window.PaymentGateway = PaymentGateway;
})(window);