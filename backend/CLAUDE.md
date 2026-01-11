# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Phases

### Phase 1: UPI Payments (CURRENT)
- Focus: UPI collect and payment flow
- Database: H2 file-based (persists merchant/contract data, transactions reset on startup)
- Flow: Create Payment → Initiate UPI Collect → Simulate Customer Approval → Payment Success
- Test Approach: Using simulated endpoints for approval testing

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
2. **Initiate UPI Collect** `POST /api/payments/upi/collect` → Generate UPI request
3. **Simulate Customer Approval** `POST /api/transactions/{id}/simulate-approval` → (Testing only)
4. **PSP Callback** `POST /api/transactions/upi/callback` → Process payment result
5. **Refund Payment** `POST /api/payments/refund` → Initiate refund transaction

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

**Test VPAs (UPI):**
- `success@upi` - Payment will succeed
- `fail@upi` - Customer rejects payment
- `timeout@upi` - Payment stays pending
- `insufficient@upi` - Insufficient funds error

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
