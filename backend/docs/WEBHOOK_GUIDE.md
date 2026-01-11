# Webhook Integration Guide

## Overview

This guide explains how to implement webhooks in the payment gateway. Webhooks allow merchants to be notified of payment state changes in real-time without polling the API.

## Benefits of Using UUIDs

The payment gateway now includes **full UUIDs** for secure webhook payloads:

### Payment UUID
- **Field**: `paymentUuid` (Full UUID, 36 characters)
- **Format**: `550e8400-e29b-41d4-a716-446655440000`
- **Use Case**: Sharing payment links via email, SMS, QR codes
- **Advantage**: Impossible to guess sequentially, more secure than numeric IDs
- **Payment Link Example**: `https://payment.yourapp.com/pay/{paymentUuid}`

### Transaction UUID
- **Field**: `transactionUuid` (Full UUID, 36 characters)
- **Format**: `6ba7b810-9dad-11d1-80b4-00c04fd430c8`
- **Use Case**: Including in webhook calls, linking to final state
- **Advantage**: Uniquely identifies transaction across all payment methods
- **Webhook Payload**: Includes both `transactionUuid` and `transactionId` for reference

---

## API Response Format

### Create Payment Response

```json
{
  "paymentId": "pay_a1b2c3d4e5f67890",
  "paymentUuid": "550e8400-e29b-41d4-a716-446655440000",
  "merchantOrderId": "ORD-1704844800000",
  "dueAmount": 100.00,
  "paidAmount": 0.00,
  "refundedAmount": 0.00,
  "currency": "INR",
  "status": "CREATED",
  "testMode": true,
  "expiresAt": "2026-01-10T20:15:10.123Z",
  "createdAt": "2026-01-10T20:00:10.123Z"
}
```

**Key Points:**
- Use `paymentUuid` for payment links
- `paymentId` is for internal reference (backward compatible)
- Both are unique and persistent across system

---

### Get Transaction Response

```json
{
  "transactionId": "txn_x9y8z7w6v5u4t3s2",
  "transactionUuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "paymentId": "pay_a1b2c3d4e5f67890",
  "txnType": "DEBIT",
  "paymentMethod": "UPI",
  "amount": 100.00,
  "status": "SUCCESS",
  "pspReferenceId": "RRN_1234567890",
  "bankReferenceId": "BANK_REF_ABC123",
  "failureReason": null,
  "createdAt": "2026-01-10T20:05:10.123Z"
}
```

**Key Points:**
- `transactionUuid` is the primary identifier for webhooks
- `transactionId` maintained for backward compatibility
- Include both in webhook payload for merchant reconciliation

---

## Payment Link Implementation

### Generate Payment Link

**Request:**
```bash
POST /api/payments
Content-Type: application/json

{
  "merchantId": "MER001",
  "merchantOrderId": "ORD-123456",
  "amount": 500.00,
  "currency": "INR",
  "customerMobile": "9876543210"
}
```

**Response:**
```json
{
  "paymentId": "pay_a1b2c3d4e5f67890",
  "paymentUuid": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CREATED",
  ...
}
```

### Create Payment Link

From the response, extract `paymentUuid`:

```
Payment Link: https://payment.yourapp.com/pay/550e8400-e29b-41d4-a716-446655440000
```

**Benefits:**
- No sequential IDs exposed to customers
- Cannot be enumerated or brute-forced
- Same link can be shared via email, SMS, WhatsApp
- Link verification: `GET /api/payments/{paymentId}` or via UUID

---

## Webhook Payload Structure

### Transaction Complete Webhook

When a transaction completes (SUCCESS or FAILED), your webhook endpoint receives:

```json
{
  "event": "transaction.completed",
  "timestamp": "2026-01-10T20:05:10.123Z",
  "signature": "sha256_hmac_signature_here",

  "data": {
    "transaction": {
      "transactionUuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "transactionId": "txn_x9y8z7w6v5u4t3s2",
      "paymentId": "pay_a1b2c3d4e5f67890",
      "paymentUuid": "550e8400-e29b-41d4-a716-446655440000",
      "merchantOrderId": "ORD-123456",
      "amount": 500.00,
      "currency": "INR",
      "status": "SUCCESS",
      "failureReason": null,
      "paymentMethod": "UPI",
      "txnType": "DEBIT",
      "pspReferenceId": "RRN_1234567890",
      "bankReferenceId": "BANK_REF_ABC123",
      "createdAt": "2026-01-10T20:05:10.123Z"
    },

    "payment": {
      "paymentId": "pay_a1b2c3d4e5f67890",
      "paymentUuid": "550e8400-e29b-41d4-a716-446655440000",
      "merchantOrderId": "ORD-123456",
      "dueAmount": 500.00,
      "paidAmount": 500.00,
      "refundedAmount": 0.00,
      "currency": "INR",
      "status": "COMPLETED",
      "createdAt": "2026-01-10T20:00:10.123Z"
    }
  }
}
```

---

## Webhook Security

### Signature Verification (Phase 2)

**Important**: Always verify the webhook signature before processing.

```javascript
// Node.js example (Phase 2 implementation)
const crypto = require('crypto');

function verifyWebhookSignature(payload, signature, secret) {
  const hmac = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');

  return hmac === signature;
}
```

### Implementation Steps

1. **Store Webhook Secret**
   - Each merchant has a unique webhook secret
   - Keep it secure (environment variable)
   - Never log or expose

2. **Verify Signature**
   - Extract `signature` from webhook header: `X-Webhook-Signature`
   - Calculate HMAC-SHA256 of payload body
   - Compare with provided signature
   - Reject if mismatch

3. **Idempotency**
   - Use `transactionUuid` as idempotency key
   - Store processed webhooks in database
   - Retry mechanism: If webhook fails, system retries with exponential backoff
   - **Your endpoint must be idempotent** - processing same webhook twice should be safe

---

## Webhook Endpoints (Phase 2)

### Register Webhook Endpoint

```bash
POST /api/merchants/{merchantId}/webhooks
Content-Type: application/json
Authorization: Bearer {apiKey}

{
  "url": "https://yourapp.com/webhooks/payment",
  "events": ["transaction.completed", "payment.expired"],
  "secret": "whsk_your_secret_key"
}
```

**Response:**
```json
{
  "webhookId": "whk_uuid_here",
  "merchantId": "MER001",
  "url": "https://yourapp.com/webhooks/payment",
  "events": ["transaction.completed", "payment.expired"],
  "active": true,
  "createdAt": "2026-01-10T20:00:10.123Z"
}
```

### Update Webhook Endpoint

```bash
PUT /api/merchants/{merchantId}/webhooks/{webhookId}
Content-Type: application/json
Authorization: Bearer {apiKey}

{
  "url": "https://yourapp.com/webhooks/payment-v2",
  "active": true
}
```

### Delete Webhook Endpoint

```bash
DELETE /api/merchants/{merchantId}/webhooks/{webhookId}
Authorization: Bearer {apiKey}
```

---

## Webhook Implementation Example

### Your Webhook Endpoint (Node.js/Express)

```javascript
const express = require('express');
const crypto = require('crypto');

const app = express();
app.use(express.json());

// Store processed webhooks to handle retries
const processedWebhooks = new Set();

app.post('/webhooks/payment', (req, res) => {
  const payload = req.body;
  const signature = req.headers['x-webhook-signature'];
  const secret = process.env.WEBHOOK_SECRET;

  // Verify signature
  const hmac = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');

  if (hmac !== signature) {
    return res.status(401).json({ error: 'Invalid signature' });
  }

  // Use transactionUuid as idempotency key
  const { transactionUuid } = payload.data.transaction;

  if (processedWebhooks.has(transactionUuid)) {
    // Already processed - return success to acknowledge delivery
    return res.json({ success: true, message: 'Webhook already processed' });
  }

  try {
    // Process transaction
    const transaction = payload.data.transaction;

    switch (transaction.status) {
      case 'SUCCESS':
        handlePaymentSuccess(transaction);
        break;
      case 'FAILED':
        handlePaymentFailure(transaction);
        break;
      default:
        console.log(`Unhandled status: ${transaction.status}`);
    }

    // Mark as processed
    processedWebhooks.add(transactionUuid);

    res.json({ success: true });
  } catch (error) {
    console.error('Webhook processing error:', error);
    // Return error - system will retry
    res.status(500).json({ error: 'Processing failed' });
  }
});

function handlePaymentSuccess(transaction) {
  console.log(`Payment successful: ${transaction.transactionUuid}`);
  console.log(`Amount: ${transaction.amount} ${transaction.currency}`);
  console.log(`Merchant Order: ${transaction.merchantOrderId}`);

  // Update database
  // Send confirmation email
  // Update order status
}

function handlePaymentFailure(transaction) {
  console.log(`Payment failed: ${transaction.transactionUuid}`);
  console.log(`Reason: ${transaction.failureReason}`);

  // Update database
  // Send failure notification
  // Trigger retry mechanism
}

app.listen(3000, () => {
  console.log('Webhook server running on port 3000');
});
```

---

## Webhook Events (Phase 2)

| Event | Description | Trigger |
|---|---|---|
| `transaction.completed` | Transaction finished (SUCCESS or FAILED) | Immediately after UPI collect response |
| `transaction.initiated` | Transaction initiated | After UPI collect request created |
| `payment.expired` | Payment expired (15 min default) | After expiry time passes |
| `payment.refunded` | Payment refunded | After refund transaction succeeds |
| `transaction.retried` | Transaction retry attempted | On manual retry |

---

## Retry Strategy (Phase 2)

### Automatic Retry

```
Attempt 1: Immediate
Attempt 2: 5 seconds later
Attempt 3: 30 seconds later
Attempt 4: 5 minutes later
Attempt 5: 30 minutes later
Attempt 6: 2 hours later

Total window: ~2.5 hours
```

### Webhook Status Tracking

```bash
GET /api/merchants/{merchantId}/webhooks/{webhookId}/logs

Response:
{
  "webhookId": "whk_uuid",
  "recent_deliveries": [
    {
      "timestamp": "2026-01-10T20:05:10Z",
      "event": "transaction.completed",
      "transactionUuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "status": "SUCCESS",
      "http_status": 200,
      "attempts": 1
    }
  ]
}
```

---

## Testing Webhooks

### Manual Webhook Trigger (Development)

```bash
POST /api/webhooks/test/{transactionId}
Authorization: Bearer {apiKey}

# Triggers webhook delivery for specified transaction
```

### Webhook Testing Tools

- **ngrok**: Expose local server to internet
  ```bash
  ngrok http 3000
  # Use ngrok URL as webhook endpoint
  ```

- **webhook.site**: Free temporary webhook URLs for testing

- **Postman**: Mock webhook calls to test your endpoint

---

## Best Practices

1. **Always Verify Signatures**
   - Never trust webhook payload without signature verification
   - Use constant-time comparison for hmac (prevent timing attacks)

2. **Be Idempotent**
   - Use `transactionUuid` as idempotency key
   - Safely handle duplicate webhooks
   - Store processed webhook UUIDs

3. **Handle Failures Gracefully**
   - Return 2xx for success even if processing partially fails
   - Return 5xx to trigger retries if critical failure
   - Log all webhook attempts

4. **Keep Endpoints Fast**
   - Acknowledge receipt immediately (< 5 seconds)
   - Do heavy processing asynchronously (queue job)
   - Don't make blocking external calls

5. **Monitor Webhook Health**
   - Track delivery success rates
   - Alert on repeated failures
   - Review logs periodically

---

## Migration from ID to UUID

### Phase 1 (Current)
- Both `paymentId` and `paymentUuid` present in responses
- `paymentId` used for API calls: `GET /api/payments/{paymentId}`
- `paymentUuid` available for payment links

### Phase 2 (Future)
- Webhooks will include both for backward compatibility
- New merchant integrations should use `paymentUuid`
- Legacy integrations can continue using `paymentId`

### Deprecation Timeline
- **2026-Q1**: Webhooks include both IDs
- **2026-Q2**: Deprecation notice for paymentId
- **2026-Q4**: paymentId support may be removed

---

## Support & Examples

For full webhook examples in different languages:
- **Node.js/JavaScript**: `/examples/webhooks/nodejs.js`
- **Python**: `/examples/webhooks/python.py`
- **Go**: `/examples/webhooks/main.go`
- **Java**: `/examples/webhooks/WebhookHandler.java`

---

## Troubleshooting

### Webhook Not Being Delivered

1. **Check endpoint URL**
   - Ensure HTTPS (for production)
   - Verify domain is accessible

2. **Check logs**
   - GET `/api/merchants/{merchantId}/webhooks/{webhookId}/logs`
   - Look for HTTP error responses

3. **Verify secret**
   - Signature verification failing?
   - Confirm webhook secret matches your stored secret

4. **Network issues**
   - Is your firewall blocking inbound requests?
   - Check with ngrok if testing locally

### Duplicate Webhooks

This is normal! Implement idempotency using `transactionUuid`:
```javascript
const seen = new Set();

if (seen.has(transactionUuid)) {
  return res.json({ success: true }); // Already processed
}
seen.add(transactionUuid);
```

### Missing Fields

Ensure you're using a recent API version. Include `Accept: application/json` header in requests.

---

## Next Steps

1. Register webhook endpoint with backend team
2. Implement signature verification in your endpoint
3. Test with manual webhook trigger
4. Monitor delivery logs
5. Implement proper error handling and retries

