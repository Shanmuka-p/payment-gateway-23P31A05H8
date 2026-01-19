const express = require('express');
const path = require('path');

const app = express();
const distPath = path.join(__dirname, 'dist');

// Serve static files from dist
app.use(express.static(distPath));

// Serve demo.html from root
app.use(express.static(__dirname));

app.get('/checkout', (req, res) => {
  const html = `<!doctype html>
  <html>
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <title>Payment Checkout</title>
      <style>
        body { margin: 0; font-family: Arial, sans-serif; background: #f8fafc; }
        .pg-iframe-shell { padding: 24px; }
        form { display: flex; flex-direction: column; gap: 12px; }
        input { padding: 10px; border: 1px solid #d4d4d4; border-radius: 8px; }
        button { padding: 12px; background: #2563eb; color: #fff; border: none; border-radius: 10px; cursor: pointer; }
        button[disabled] { background: #9ca3af; cursor: not-allowed; }
      </style>
    </head>
    <body>
      <div id="root"></div>
      <script src="/iframe.js"></script>
    </body>
  </html>`;
  res.send(html);
});

const port = process.env.PORT || 3001;
app.listen(port, () => console.log(`Checkout widget server listening on ${port}`));
