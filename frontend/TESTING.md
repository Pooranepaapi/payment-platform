# End-to-End UPI Payment Testing Guide

This guide walks through testing the complete UPI payment flow from merchant initiation to payment confirmation.

## Prerequisites

- Backend running on `http://localhost:8080`
- Frontend running on `http://localhost:5173`
- H2 database configured and initialized with test merchants
- No external UPI integration required (all endpoints are simulated)

## Phase 1: UPI Payments Architecture

```
Merchant Portal → Create Payment → UPI Collect → Customer Approval Simulation → Transaction Status
    (MER001/MER002) → (Backend)   → (Backend)  → (Frontend simulates)      → (Frontend polls)
```

### Test Merchants (Auto-seeded by DataInitializer)

| ID | Name | VPA | Contract |
|---|---|---|---|
| MER001 | Test Store | teststore@rbl | RBL UPI |
| MER002 | Demo Shop | demoshop@hdfc | HDFC UPI |

### Test Customer VPAs (UPI Simulator)

| VPA | Behavior | Result |
|---|---|---|
| `success@upi` | Always succeeds | Status: SUCCESS |
| `fail@upi` | Always fails | Status: FAILED |
| `timeout@upi` | Stays pending | Status: INITIATED (no resolution) |
| `insufficient@upi` | Insufficient funds | Status: FAILED with reason |

---

## Step-by-Step Testing Guide

### **Step 1: Start the Backend**

```bash
cd C:\Users\Rohit\development\payment-backend

# Clean and compile (first time only)
mvn clean compile

# Run the application
mvn spring-boot:run
```

**Expected Console Output:**
```
=================================================
   PAYMENT GATEWAY - UPI POC (Phase 1)
=================================================

Test Merchants:
  - MER001: Test Store (teststore@rbl)
  - MER002: Demo Shop (demoshop@hdfc)

Test VPAs for UPI Simulator:
  - success@upi   : Always succeeds
  - fail@upi      : Always fails
  - timeout@upi   : Stays pending
  - insufficient@upi : Insufficient funds

API Endpoints:
  POST /api/payments           - Create payment
  GET  /api/payments/{id}      - Get payment
  POST /api/payments/upi/collect - Initiate UPI collect
  POST /api/payments/refund    - Initiate refund
  GET  /api/transactions/{id}  - Get transaction
  POST /api/transactions/{id}/simulate-approval
                               - Simulate customer approval

=================================================
```

**Verify H2 Database Setup:**
- Open browser: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./paymentdb`
- Username: `sa`
- Password: (leave blank)
- Click "Connect"
- You should see tables: `MERCHANTS`, `CONTRACTS`, `PAYMENTS`, `TRANSACTIONS`

### **Step 2: Start the Frontend**

In a **new terminal window**:

```bash
cd C:\Users\Rohit\development\payment-frontend

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev
```

**Expected Output:**
```
  VITE v5.4.21  ready in 123 ms

  ➜  Local:   http://localhost:5173/
  ➜  press h + enter to show help
```

### **Step 3: Test Successful UPI Payment Flow**

#### 3.1 Navigate to Merchant Portal
- Open browser: `http://localhost:5173`
- You should see "Merchant Portal" page
- Click "Proceed to Checkout"

#### 3.2 Configure Payment Details
You'll see a form with pre-filled fields:
- **Merchant ID**: MERCHANT_001
- **Order ID**: Auto-generated (ORD-{timestamp})
- **Amount**: 100.00
- **Currency**: USD
- **Customer Email**: customer@example.com

**Action**: Click "Proceed to Checkout" button

#### 3.3 Checkout Page - Select UPI
You'll be on the Checkout page with two tabs: "Card" and "UPI"

- Ensure **UPI tab is selected**
- **Customer VPA**: Select `success@upi` from dropdown
- Click **"Pay with UPI"** button

#### 3.4 Observe Payment Flow
The frontend will execute this sequence:

1. **Creating payment...** (POST /api/payments)
   - Backend creates Payment entity
   - Returns: `paymentId`

2. **Sending UPI collect request...** (POST /api/payments/upi/collect)
   - Backend creates UPI transaction
   - Returns: `transactionId`

3. **Waiting for customer approval...** (1.5 second delay simulating customer interaction)

4. **Simulating customer approval...** (POST /api/transactions/{id}/simulate-approval)
   - Backend processes the simulated approval
   - Updates transaction status

5. **Processing payment...** (Polling GET /api/transactions/{id})
   - Frontend polls every 1 second (max 10 attempts)
   - Checks for final status: SUCCESS, FAILED, or timeout

#### 3.5 View Transaction Details
After payment completes, you'll be redirected to **Transaction Details page** showing:

- **Status**: SUCCESS (green banner)
- **Type**: UPI_COLLECT
- **Amount**: 100.00 USD
- **PSP Reference ID**: Unique transaction ID from backend
- **Payment Method**: UPI
- **Buttons**:
  - Refresh (to reload transaction status)
  - New Payment (back to merchant portal)

**Example Response (with UUIDs):**
```json
{
  "transactionId": "txn_a1b2c3d4e5f67890",
  "transactionUuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "paymentId": "pay_x9y8z7w6v5u4t3s2",
  "paymentUuid": "550e8400-e29b-41d4-a716-446655440000",
  "txnType": "DEBIT",
  "paymentMethod": "UPI",
  "amount": 100.00,
  "currency": "USD",
  "status": "SUCCESS",
  "pspReferenceId": "RRN_abcd1234",
  "bankReferenceId": "BANK_ref123",
  "createdAt": "2026-01-10T12:34:56.789Z"
}
```

**Key Fields Explained:**
- `transactionUuid`: Full UUID for webhook calls (36 characters)
- `transactionId`: Shortened ID for backward compatibility (16 characters)
- `paymentUuid`: Full UUID for payment links (36 characters)
- `paymentId`: Shortened ID for backward compatibility (16 characters)

---

### **Step 4: Test Failed Payment**

Repeat Steps 3.1-3.3, but in Step 3.3:
- Select **`fail@upi`** as Customer VPA
- Click "Pay with UPI"

**Expected Result:**
- Status page shows: **FAILED** (red banner)
- Failure Reason: "Simulated failure for fail@upi"
- Page displays error message
- "New Payment" button allows retry

---

### **Step 5: Test Pending/Timeout Payment**

Repeat Steps 3.1-3.3, but in Step 3.3:
- Select **`timeout@upi`** as Customer VPA
- Click "Pay with UPI"

**Expected Result:**
- Frontend polls for up to 10 seconds
- Status remains: **INITIATED** (yellow banner)
- After 10 polling attempts (10 seconds), timeout occurs
- Page navigates to transaction details showing INITIATED status
- "Refresh" button allows manual polling

---

### **Step 6: Test Insufficient Funds**

Repeat Steps 3.1-3.3, but in Step 3.3:
- Select **`insufficient@upi`** as Customer VPA
- Click "Pay with UPI"

**Expected Result:**
- Status page shows: **FAILED** (red banner)
- Failure Reason: "Insufficient funds"

---

## API Testing (Manual - using Postman/cURL)

If you want to test APIs directly:

### Create Payment
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "MER001",
    "merchantOrderId": "ORDER_123",
    "amount": 100.00,
    "currency": "INR",
    "testMode": true,
    "customerMobile": "9999999999"
  }'
```

**Response:**
```json
{
  "paymentId": "PAY_1234567890",
  "merchantOrderId": "ORDER_123",
  "dueAmount": 100.00,
  "paidAmount": 0.00,
  "refundedAmount": 0.00,
  "currency": "INR",
  "status": "PENDING",
  "createdAt": "2026-01-10T12:34:56.789Z"
}
```

### Initiate UPI Collect
```bash
curl -X POST http://localhost:8080/api/payments/upi/collect \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "PAY_1234567890",
    "customerVpa": "success@upi",
    "contractId": "CON001"
  }'
```

**Response:**
```json
{
  "transactionId": "TXN_9876543210",
  "paymentId": "PAY_1234567890",
  "txnType": "UPI_COLLECT",
  "paymentMethod": "UPI",
  "amount": 100.00,
  "status": "INITIATED",
  "createdAt": "2026-01-10T12:34:56.789Z"
}
```

### Simulate Customer Approval
```bash
curl -X POST http://localhost:8080/api/transactions/TXN_9876543210/simulate-approval \
  -H "Content-Type: application/json" \
  -d '{"customerVpa": "success@upi"}'
```

### Get Transaction Status
```bash
curl http://localhost:8080/api/transactions/TXN_9876543210
```

**Response:**
```json
{
  "transactionId": "TXN_9876543210",
  "paymentId": "PAY_1234567890",
  "status": "SUCCESS",
  "amount": 100.00,
  "pspReferenceId": "RRN_abcd1234",
  "bankReferenceId": "BANK_ref123"
}
```

---

## Payment Links with UUIDs

### What are Payment UUIDs?

Each payment now includes a `paymentUuid` field - a full 36-character UUID that can be safely shared.

**Benefits:**
- Non-sequential (impossible to guess)
- Unique across all payments
- Can be used in URLs/QR codes
- Perfect for sharing via email, SMS, WhatsApp

### Creating a Payment Link

When you create a payment via the API:

```bash
POST /api/payments
{
  "merchantId": "MER001",
  "merchantOrderId": "ORD-123",
  "amount": 500.00,
  "currency": "INR"
}
```

**Response includes:**
```json
{
  "paymentId": "pay_a1b2c3d4e5f67890",
  "paymentUuid": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CREATED"
}
```

### Use the UUID to create payment link:

```
https://payment.yourapp.com/pay/550e8400-e29b-41d4-a716-446655440000
```

### Sharing Options:

1. **Email**: Include link in payment notification email
2. **SMS**: Send link via SMS for mobile payments
3. **QR Code**: Encode UUID in QR code for scan-to-pay
4. **Messaging**: Share via WhatsApp, Telegram, etc.

### Current Testing:

In the test frontend, payment links are generated dynamically but not yet exposed. In Phase 2, merchant dashboard will show:

```
Order #ORD-123
Amount: ₹500
Status: PENDING

Payment Link: https://payment.yourapp.com/pay/550e8400-e29b-41d4-a716-446655440000

✓ Share via Email
✓ Share via SMS
✓ Generate QR Code
```

### Database Storage:

Check H2 console to see UUIDs in PAYMENT table:

```sql
SELECT payment_id, payment_uuid, merchant_order_id, due_amount
FROM payment
ORDER BY created_at DESC;
```

Example output:
```
| payment_id           | payment_uuid                         | merchant_order_id | due_amount |
|---|---|---|---|
| pay_a1b2c3d4e5f67890 | 550e8400-e29b-41d4-a716-446655440000 | ORD-123           | 500.00     |
| pay_x9y8z7w6v5u4t3s2 | 6ba7b810-9dad-11d1-80b4-00c04fd430c8 | ORD-124           | 100.00     |
```

---

## Troubleshooting

### Backend Won't Start

**Error**: `JDBC URL: jdbc:mysql://localhost:3306/payment_gateway`
- **Cause**: Old configuration still using MySQL
- **Fix**: Ensure you've rebuilt with `mvn clean compile`

**Error**: `H2 Driver not found`
- **Cause**: H2 dependency not downloaded
- **Fix**: Run `mvn clean install` to download dependencies

### Frontend Can't Connect to Backend

**Error**: `Failed to fetch http://localhost:8080/...`
- **Check**: Is backend running on port 8080? Check console output
- **Check**: CORS is enabled (should work with any origin in Phase 1)
- **Check**: No firewall blocking localhost:8080

**Browser Console**: `POST /api/payments net::ERR_CONNECTION_REFUSED`
- **Fix**: Start backend first, then frontend

### Transaction Stuck in "Processing"

**Cause**: VPA is `timeout@upi` which simulates a pending state
- **Expected**: Will timeout after 10 seconds
- **Fix**: Navigate back and try `success@upi` VPA

### H2 Console Shows No Data

**Check**: Are you connected to the right database?
- JDBC URL should be: `jdbc:h2:file:./paymentdb`
- File location: `./paymentdb.mv.db` in the `payment-backend` directory

**Check**: Query the MERCHANTS table:
```sql
SELECT * FROM MERCHANTS;
```
Should return 2 rows (MER001, MER002)

---

## Expected Data Flow Summary

### Successful UPI Payment (success@upi)

```
Frontend                          Backend
  │
  ├─ POST /api/payments ────────→ Create Payment (PENDING)
  │                       ← PaymentId
  │
  ├─ POST /api/payments/upi/collect ──→ Create Transaction (INITIATED)
  │                       ← TransactionId
  │
  ├─ [1.5s delay]
  │
  ├─ POST /api/transactions/{id}/simulate-approval ──→ Process UPI (SUCCESS)
  │
  ├─ GET /api/transactions/{id} ──→ Poll status
  │                       ← SUCCESS
  │
  └─ Render Transaction Details Page (SUCCESS status)
```

### Failed Payment (fail@upi)

```
Frontend                          Backend
  │
  ├─ POST /api/payments ────────→ Create Payment (PENDING)
  │                       ← PaymentId
  │
  ├─ POST /api/payments/upi/collect ──→ Create Transaction (INITIATED)
  │                       ← TransactionId
  │
  ├─ [1.5s delay]
  │
  ├─ POST /api/transactions/{id}/simulate-approval ──→ Process UPI (FAILED)
  │
  ├─ GET /api/transactions/{id} ──→ Poll status
  │                       ← FAILED + failureReason
  │
  └─ Render Transaction Details Page (FAILED status)
```

---

## Next Steps (Phase 2)

Future card payment flow will include:
- Card tokenization
- 3DS authentication (OTP verification)
- Authorization hold
- Capture (explicit post-auth)
- Void (cancel authorization)
- Refund (against captured transaction)

These require additional endpoints and database migration to MySQL.

---

## Notes

- **DataInitializer**: Runs on every backend startup and creates test merchants if they don't exist
- **Transaction Persistence**: Transactions are created fresh on each startup (stored in H2)
- **Merchant Persistence**: Merchant and contract data is persisted in H2 file database
- **Testing**: All UPI interactions are simulated; no real UPI network calls are made
- **CORS**: All origins allowed in Phase 1 for development convenience
