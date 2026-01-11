# 📊 ER Diagram & API Specification

---

## ER Diagram (ASCII Art)

```
┌─────────────────────┐
│  MasterMerchant     │
│  ─────────────────  │
│  id (PK)            │
│  masterMerchantUuid │
│  name               │
│  status             │
│  created_at         │
│  deleted_at         │
└──────────┬──────────┘
           │ 1
           │
           │ (1:N)
           │
           ▼ N
┌─────────────────────┐
│   Merchant          │
│  ─────────────────  │
│  id (PK)            │
│  merchantUuid       │
│  masterMerchantId   │──────┐
│  name               │      │ FK
│  vpa (UNIQUE)       │      │
│  status             │      │
│  platformFeePercent │      │
│  created_at         │      │
│  deleted_at         │      │
└──────────┬──────────┘      │
           │                  │
           │ 1                │
           │                  │
           │ (1:N)            │
           │                  │
           ▼ N                │
┌─────────────────────┐      │
│  PaymentOrder       │      │
│  ─────────────────  │      │
│  id (PK)            │      │
│  paymentUuid (UNIQ) │      │
│  merchantId         │──────┘
│  status             │ (Composite Index)
│  amountInPaise      │ (merchant_id, status, createdAt)
│  currency           │
│  platformFeeInPaise │
│  merchantNetInPaise │
│  expires_at         │
│  created_at         │
└──────┬──────────────┘
       │
       │ 1
       │
       ├─────────────────────────┬────────────────────┬─────────────────────┐
       │                         │                    │                     │
       ▼ (1:1)                   ▼ (1:N)              ▼ (1:N)               ▼ (1:N)
  ┌──────────┐           ┌──────────────┐    ┌──────────────┐      ┌──────────────┐
  │ QrCode   │           │PaymentEvent  │    │PaymentTxn    │      │ RefundOrder  │
  ├──────────┤           ├──────────────┤    ├──────────────┤      ├──────────────┤
  │ id (PK)  │           │ id (PK)      │    │ id (PK)      │      │ id (PK)      │
  │ paymentId│           │ paymentId    │    │ paymentId    │      │ paymentId    │
  │ qrType   │           │ paymentUuid  │    │ transactionId│      │ refundUuid   │
  │ qrImage  │           │ eventType    │    │ pspTxnId     │      │ status       │
  │ upiIntent│           │ metadata     │    │ pspAppCode   │      │ refundAmount │
  │ expiresAt│           │ created_at   │    │ status       │      │ reason       │
  │ created  │           │              │    │ pspName      │      │ created_at   │
  └──────────┘           │              │    │ created_at   │      │              │
    (1:1)                │              │    │              │      └──────────────┘
    CASCADE              └──────────────┘    └──────────────┘
                            (1:N)               (1:N)
                            RESTRICT            RESTRICT
                            (audit trail)       (PSP interaction)
```

---

## Relationship Details

| From | To | Type | Cardinality | FK Action | Notes |
|---|---|---|---|---|---|
| MasterMerchant | Merchant | Parent-Child | 1:N | RESTRICT | Master owns children |
| Merchant | PaymentOrder | Parent-Child | 1:N | RESTRICT | Merchant receives payments |
| PaymentOrder | QrCode | One-to-One | 1:1 | CASCADE | Each payment has 1 QR |
| PaymentOrder | PaymentEvent | One-to-Many | 1:N | RESTRICT | Audit trail (immutable) |
| PaymentOrder | PaymentTransaction | One-to-Many | 1:N | RESTRICT | PSP interaction record |
| PaymentOrder | RefundOrder | One-to-Many | 1:N | RESTRICT | Multiple partial refunds |

---

# 🔌 API Specification (Phase-1)

## Base URL

```
http://localhost:8080/api/v1
```

## Common Headers

```
Content-Type: application/json
Accept: application/json
```

## Response Format (All Endpoints)

### Success (2xx)

```json
{
  "success": true,
  "data": { /* entity or list */ },
  "message": "Operation successful"
}
```

### Error (4xx, 5xx)

```json
{
  "success": false,
  "error": {
    "code": "PAYMENT_NOT_FOUND",
    "message": "Payment not found",
    "details": "Payment with UUID ... does not exist"
  },
  "timestamp": "2026-01-10T10:05:30Z"
}
```

---

## Merchant Endpoints

### 1. Create Merchant

**Request:**
```
POST /api/v1/merchants
Content-Type: application/json

{
  "masterMerchantId": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Store A",
  "vpa": "storea@axis",
  "platformFeePercentage": 2.50
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "merchantId": "550e8400-e29b-41d4-a716-446655440002",
    "name": "Store A",
    "vpa": "storea@axis",
    "status": "ACTIVE",
    "platformFeePercentage": 2.50,
    "createdAt": "2026-01-10T10:00:00Z"
  }
}
```

**Errors:**
- `400 Bad Request`: Invalid VPA format
- `409 Conflict`: VPA already exists
- `404 Not Found`: MasterMerchant not found

---

### 2. Get Merchant

**Request:**
```
GET /api/v1/merchants/{merchantId}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "merchantId": "550e8400-e29b-41d4-a716-446655440002",
    "masterMerchantId": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Store A",
    "vpa": "storea@axis",
    "status": "ACTIVE",
    "platformFeePercentage": 2.50,
    "createdAt": "2026-01-10T10:00:00Z",
    "updatedAt": "2026-01-10T10:00:00Z"
  }
}
```

**Errors:**
- `404 Not Found`: Merchant not found

---

### 3. List Merchants (by MasterMerchant)

**Request:**
```
GET /api/v1/master-merchants/{masterMerchantId}/merchants
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "merchantId": "550e8400-e29b-41d4-a716-446655440002",
      "name": "Store A",
      "vpa": "storea@axis",
      "status": "ACTIVE",
      "platformFeePercentage": 2.50
    },
    {
      "merchantId": "550e8400-e29b-41d4-a716-446655440003",
      "name": "Store B",
      "vpa": "storeb@hdfc",
      "status": "ACTIVE",
      "platformFeePercentage": 2.50
    }
  ]
}
```

---

## Payment Endpoints

### 1. Create Payment (with QR Generation)

**Request:**
```
POST /api/v1/payments
Content-Type: application/json

{
  "merchantId": "550e8400-e29b-41d4-a716-446655440002",
  "amountInPaise": 10050,
  "currency": "INR",
  "externalOrderId": "ORDER_12345",
  "description": "Tea + Coffee"
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "paymentId": "550e8400-e29b-41d4-a716-446655440005",
    "merchantId": "550e8400-e29b-41d4-a716-446655440002",
    "merchantName": "Store A",
    "amountInPaise": 10050,
    "amount": 100.50,
    "currency": "INR",
    "platformFeeInPaise": 250,
    "platformFee": 2.50,
    "merchantNetInPaise": 9800,
    "merchantNet": 98.00,
    "status": "QR_GENERATED",
    "expiresAt": "2026-01-10T10:15:30Z",
    "createdAt": "2026-01-10T10:00:30Z"
  }
}
```

**Errors:**
- `400 Bad Request`: Invalid amount/currency
- `404 Not Found`: Merchant not found
- `409 Conflict`: Merchant inactive/suspended

---

### 2. Get Payment Status (with Expiry Check)

**Request:**
```
GET /api/v1/payments/{paymentId}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "paymentId": "550e8400-e29b-41d4-a716-446655440005",
    "merchantId": "550e8400-e29b-41d4-a716-446655440002",
    "merchantName": "Store A",
    "amountInPaise": 10050,
    "amount": 100.50,
    "currency": "INR",
    "status": "PENDING",  // or SUCCESS, FAILED, EXPIRED
    "expiresAt": "2026-01-10T10:15:30Z",
    "createdAt": "2026-01-10T10:00:30Z",
    "updatedAt": "2026-01-10T10:05:00Z",
    "transaction": {
      "transactionId": "550e8400-e29b-41d4-a716-446655440006",
      "pspTransactionId": "PSP_12345",
      "status": "SUCCESS",
      "pspName": "AXIS",
      "approvalCode": "ABC123"
    }
  }
}
```

**Notes:**
- If `now() > expiresAt` and status is PENDING → auto-transition to EXPIRED
- If transaction exists → include transaction details
- Status can be: CREATED, QR_GENERATED, PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED

**Errors:**
- `404 Not Found`: Payment not found

---

### 3. Get QR Code

**Request:**
```
GET /api/v1/payments/{paymentId}/qr
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "qrImageBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
    "qrImageSvg": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\">...</svg>",
    "upiIntent": "upi://pay?pa=storea@axis&pn=Store%20A&am=100.50&tn=ORDER_12345&tr=550e8400...",
    "expiresAt": "2026-01-10T10:15:30Z"
  }
}
```

**Errors:**
- `404 Not Found`: Payment or QR not found

---

### 4. List Payments (by Merchant)

**Request:**
```
GET /api/v1/merchants/{merchantId}/payments?status=SUCCESS&limit=20&offset=0
```

**Query Parameters:**
- `status`: Filter by status (optional)
- `limit`: Page size (default: 20)
- `offset`: Pagination offset (default: 0)

**Response (200):**
```json
{
  "success": true,
  "data": {
    "total": 150,
    "limit": 20,
    "offset": 0,
    "payments": [
      {
        "paymentId": "550e8400-e29b-41d4-a716-446655440005",
        "amountInPaise": 10050,
        "amount": 100.50,
        "status": "SUCCESS",
        "createdAt": "2026-01-10T10:00:30Z"
      },
      /* ... more payments ... */
    ]
  }
}
```

---

### 5. Cancel Payment

**Request:**
```
POST /api/v1/payments/{paymentId}/cancel
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "paymentId": "550e8400-e29b-41d4-a716-446655440005",
    "status": "CANCELLED",
    "cancelledAt": "2026-01-10T10:05:00Z"
  }
}
```

**Errors:**
- `400 Bad Request`: Cannot cancel (status not CREATED/QR_GENERATED)
- `404 Not Found`: Payment not found

---

## Transaction Endpoints

### 1. Get Transaction Details

**Request:**
```
GET /api/v1/transactions/{transactionId}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "transactionId": "550e8400-e29b-41d4-a716-446655440006",
    "paymentId": "550e8400-e29b-41d4-a716-446655440005",
    "pspTransactionId": "PSP_12345",
    "status": "SUCCESS",
    "pspName": "AXIS",
    "approvalCode": "ABC123",
    "createdAt": "2026-01-10T10:05:00Z",
    "updatedAt": "2026-01-10T10:05:15Z"
  }
}
```

**Errors:**
- `404 Not Found`: Transaction not found

---

## Refund Endpoints

### 1. Create Refund

**Request:**
```
POST /api/v1/payments/{paymentId}/refunds
Content-Type: application/json

{
  "refundAmountInPaise": 5025,  // Can be partial (₹50.25 of ₹100.50)
  "reason": "Customer requested partial refund"
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "refundId": "550e8400-e29b-41d4-a716-446655440007",
    "paymentId": "550e8400-e29b-41d4-a716-446655440005",
    "refundAmountInPaise": 5025,
    "refundAmount": 50.25,
    "status": "INITIATED",
    "reason": "Customer requested partial refund",
    "createdAt": "2026-01-10T10:10:00Z"
  }
}
```

**Errors:**
- `400 Bad Request`: Refund amount exceeds payment amount
- `404 Not Found`: Payment not found
- `409 Conflict`: Payment status doesn't allow refunds

---

### 2. Get Refund Status

**Request:**
```
GET /api/v1/refunds/{refundId}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "refundId": "550e8400-e29b-41d4-a716-446655440007",
    "paymentId": "550e8400-e29b-41d4-a716-446655440005",
    "refundAmountInPaise": 5025,
    "status": "SUCCESS",
    "createdAt": "2026-01-10T10:10:00Z",
    "updatedAt": "2026-01-10T10:10:15Z"
  }
}
```

---

## Audit Trail Endpoints

### 1. Get Payment Events (Audit Trail)

**Request:**
```
GET /api/v1/payments/{paymentId}/events
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "eventId": 1,
      "eventType": "CREATED",
      "createdAt": "2026-01-10T10:00:30Z",
      "metadata": {}
    },
    {
      "eventId": 2,
      "eventType": "QR_GENERATED",
      "createdAt": "2026-01-10T10:00:31Z",
      "metadata": {}
    },
    {
      "eventId": 3,
      "eventType": "PENDING",
      "createdAt": "2026-01-10T10:00:32Z",
      "metadata": {
        "source": "UPI_APP"
      }
    },
    {
      "eventId": 4,
      "eventType": "SUCCESS",
      "createdAt": "2026-01-10T10:05:15Z",
      "metadata": {
        "pspTransactionId": "PSP_12345",
        "pspApprovalCode": "ABC123",
        "psp": "AXIS",
        "approvalTime": "2026-01-10T10:05:10Z"
      }
    }
  ]
}
```

---

## Error Codes

| Code | HTTP | Description |
|---|---|---|
| PAYMENT_NOT_FOUND | 404 | Payment does not exist |
| MERCHANT_NOT_FOUND | 404 | Merchant does not exist |
| MERCHANT_INACTIVE | 409 | Merchant is inactive/suspended |
| INVALID_AMOUNT | 400 | Amount must be > 0 |
| INVALID_VPA | 400 | Invalid UPI VPA format |
| DUPLICATE_VPA | 409 | VPA already registered |
| INVALID_TRANSITION | 400 | Invalid payment status transition |
| REFUND_EXCEEDS_AMOUNT | 400 | Refund amount > payment amount |
| PAYMENT_EXPIRED | 410 | Payment has expired |
| INTERNAL_ERROR | 500 | Server error |

---

## Status Codes Reference

| Code | Meaning |
|---|---|
| 200 | OK (successful read/update) |
| 201 | Created (successful POST) |
| 400 | Bad Request (validation error) |
| 404 | Not Found (resource doesn't exist) |
| 409 | Conflict (duplicate/invalid state) |
| 410 | Gone (payment expired) |
| 500 | Internal Server Error |

---

This API spec is **complete for Phase-1** and **backward-compatible** for Phase-2 additions.
