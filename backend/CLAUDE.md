# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Phases

### Phase 1: UPI Payments (CURRENT)
- Focus: UPI collect and payment flow
- Database: H2 file-based (persists merchant/contract data, transactions reset on startup)
- Flow: Create Payment → Initiate UPI Collect → Simulator Callback → Payment Success
- Test Approach: External simulator service (port 8181) handles bank simulation with async callbacks
- Architecture: Backend (8080) → Simulator (8181) → Callback to Backend

### Phase 2: Card Payments with 3DS (Future)
- Focus: Card authorization, capture, void, refund with 3DS authentication
- Database: Migration to MySQL with persistent transactions
- New Endpoints: `/api/payments/auth`, `/api/payments/sale`, `/api/transactions/{id}/capture`, etc.
- Components: ThreeDSController, ThreeDSService, OTP verification flow

---

## Build & Run Commands

```bash
# Compile
mvn compile

# Run application (port 8080)
mvn spring-boot:run

# Run tests
mvn test

# Run single test class
mvn test -Dtest=ClassName

# Run single test method
mvn test -Dtest=ClassName#methodName

# Package as JAR
mvn package

# Clean build
mvn clean compile
```

## Architecture Overview

This is a payment processing backend supporting UPI payments in Phase 1, with planned support for card payments with 3DS in Phase 2.

### Layer Structure

```
Controllers (HTTP) → Services (Business Logic) → Repositories (Data) → H2 Database (Phase 1)
                           ↓
                    SimulatorClient → External Simulator Service (port 8181)
```

### Simulator Service (Separate Project)

The `simulator` project is a separate Spring Boot application that simulates bank PSP responses:

**Location:** `payment-platform/simulator`
**Port:** 8181

**Supported Banks:**
- RBLUPI (RBL Bank) - 1.5s callback delay
- HDFCUPI (HDFC Bank) - 2.0s callback delay
- KOTAKUPI (Kotak Bank) - 2.5s callback delay

**Communication Flow:**
1. Backend calls `POST /api/simulator/upi/collect` with PaymentType
2. Simulator returns PENDING status with PSP reference
3. Simulator async calls backend's callback endpoint after delay
4. Backend updates transaction and payment status

**Run Simulator:**
```bash
cd simulator
mvn spring-boot:run
```

### Key Components

**Controllers** (`org.personal.controller`):
- `PaymentController` - Creates payments, manages UPI collect, refunds
- `TransactionController` - Transaction queries, UPI callbacks, customer approval simulation

**Services** (`org.personal.service`):
- `PaymentService` - Orchestrates payment and UPI flows, transaction state management
- Other services TBD in Phase 2

**Entities** (`org.personal.entity`):
- `Payment` - Payment master with merchant order ID, amounts, status
- `Transaction` - Individual transactions (UPI collect, refund, etc.)
- `Merchant` - Merchant data (to be initialized via DataInitializer)
- `Contract` - Contract/subscription details (to be initialized via DataInitializer)

### Phase 1: UPI Payment Flow

1. **Create Payment** `POST /api/payments` → Create payment record with initial status
2. **Initiate UPI Collect** `POST /api/payments/upi/collect` → Backend calls simulator, returns PENDING
3. **Simulator Callback** `POST /api/transactions/upi/callback` → Simulator sends async callback (1.5-2.5s)
4. **Payment Success/Failure** → Backend updates payment status based on callback
5. **Refund Payment** `POST /api/payments/refund` → Initiate refund (also via simulator)

**Note:** The `simulate-approval` endpoint is now for manual testing/recovery only. Normal flow uses automatic callbacks from the simulator service.

### Transaction States (Phase 1 - UPI)

`PENDING` → `PROCESSING` → `SUCCESS` / `FAILED`
                                ↓
                            `REFUNDED`

### Database Configuration

**Phase 1 (Current):**
```
Type: H2 File-based
URL: jdbc:h2:file:./paymentdb
Location: ./paymentdb.mv.db (file on disk)
Data Persistence: Merchant/Contract data persists, transactions can reset on startup
Initialization: DataInitializer script seeds merchants, contracts, bank accounts
```

**Phase 2 (Future):**
```
Type: MySQL
URL: jdbc:mysql://localhost:3306/payment_gateway
```

### Test Data

**Common Test VPAs (All Banks):**
- `success@upi` - Payment will succeed
- `fail@upi` - Customer rejects payment
- `timeout@upi` - Payment stays pending (no callback)
- `insufficient@upi` - Insufficient funds error

**Bank-Specific Test VPAs:**
- RBL: `rbl.success@rbl`, `rbl.fail@rbl`, `rbl.timeout@rbl`, `rbl.lowbal@rbl`
- HDFC: `hdfc.success@hdfc`, `hdfc.fail@hdfc`, `hdfc.timeout@hdfc`, `hdfc.declined@hdfc`, `hdfc.nobal@hdfc`
- Kotak: `kotak.success@kotak`, `kotak.fail@kotak`, `kotak.timeout@kotak`, `kotak.blocked@kotak`

### API Endpoints (Phase 1)

- `POST /api/payments` - Create new payment
- `GET /api/payments/{paymentId}` - Get payment details
- `POST /api/payments/upi/collect` - Initiate UPI collect request
- `GET /api/payments/{paymentId}/transactions` - Get all transactions for payment
- `POST /api/payments/refund` - Initiate refund
- `GET /api/transactions/{transactionId}` - Get transaction details
- `POST /api/transactions/upi/callback` - Receive PSP callback (internal)
- `POST /api/transactions/{transactionId}/simulate-approval` - Simulate customer approval (testing only)

### UUID Support (Phase 1)

Every Payment and Transaction now includes full UUIDs for secure sharing and webhook handling:

**Payment UUID:**
- Field: `paymentUuid` (36-character UUID, e.g., `550e8400-e29b-41d4-a716-446655440000`)
- Use: Generate payment links that can be shared via email/SMS/QR codes
- Format: `https://payment.yourapp.com/pay/{paymentUuid}`
- Benefits: Non-sequential, impossible to guess, secure

**Transaction UUID:**
- Field: `transactionUuid` (36-character UUID, e.g., `6ba7b810-9dad-11d1-80b4-00c04fd430c8`)
- Use: Include in webhook payloads for state change notifications
- Format: Used as idempotency key in webhook processing
- Benefits: Uniquely identifies transaction across payment methods

**Backward Compatibility:**
- Both `paymentId` and `paymentUuid` returned in all responses
- Both `transactionId` and `transactionUuid` returned in all responses
- Existing API calls using `paymentId` continue to work
- New integrations should prefer UUID for payment links

**Webhook Integration:**
- See `docs/WEBHOOK_GUIDE.md` for complete webhook documentation
- Includes signature verification examples
- Idempotency patterns using UUIDs
- Phase 2 webhook implementation details

### Development Notes

**H2 Console Access:**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./paymentdb`
- Username: `sa`
- Password: (leave blank)

**Data Initialization:**
- DataInitializer runs on startup
- Seeds merchant profiles, contracts, and bank accounts
- Transactions are NOT persisted between restarts (reset on startup)

**Frontend Integration:**
- Frontend (port 5173) communicates with backend (port 8080)
- CORS enabled for all origins
- Merchant data is manually entered/hardcoded in MerchantPage.jsx
- Stores merchant data in sessionStorage before checkout

**Logging:**
- Application logs set to DEBUG level for package `org.personal`
- Check console output for request/response details
