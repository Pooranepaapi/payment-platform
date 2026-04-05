# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Structure

Three independent projects, each run separately:

| Project | Language | Port | Purpose |
|---------|----------|------|---------|
| `backend/` | Java 25, Spring Boot 4.0.2 | 8080 | Payment API, business logic, H2 DB |
| `frontend/` | TypeScript, React 19, Vite | 5173 | Checkout UI |
| `simulator/` | Java 25, Spring Boot 4.0.2 | 8181 | Bank PSP simulator (separate microservice) |

## Build & Run Commands

### Backend
```bash
cd backend
mvn spring-boot:run          # Start on port 8080
mvn test                     # Run all tests
mvn test -Dtest=ClassName    # Run single test class
mvn test -Dtest=ClassName#methodName  # Run single test method
mvn clean compile            # Clean build
```

### Frontend
```bash
cd frontend
npm install
npm run dev     # Start on port 5173
npm run build
npm run lint
```

### Simulator (required for end-to-end payment flow)
```bash
cd simulator
mvn spring-boot:run          # Start on port 8181
```

## Architecture

### Request Flow
```
Frontend (5173) → Backend (8080) → Simulator (8181)
                                        ↓ async callback (1.5–2.5s)
                  Backend (8080) ←──────┘
```

### Backend Layers
```
@RestController → @Service → @Repository → H2 File DB (./paymentdb.mv.db)
                     ↓
              SimulatorClient → POST http://localhost:8181/api/simulator/upi/collect
```

### Phase 1 Payment Flow
1. `POST /api/v1/payments` — create PaymentOrder (status: CREATED)
2. `POST /api/v1/payments/{id}/qr` — generate QR code (status: QR_GENERATED)
3. `POST /api/payments/upi/collect` — backend calls simulator, returns PENDING
4. Simulator async callback → `POST /api/transactions/upi/callback` — updates to SUCCESS/FAILED
5. Frontend polls `GET /api/v1/payments/{id}` until terminal state

### Payment Status Machine
`CREATED → QR_GENERATED → PENDING → SUCCESS / FAILED / EXPIRED / CANCELLED`

### Amount Handling
All amounts stored as **paise** (₹1 = 100 paise, BIGINT). Never use floating point for amounts.

## Key Backend Packages

- `org.personal.controller` — REST endpoints (PaymentV1Controller, PaymentController, TransactionController)
- `org.personal.service` — Business logic; `PaymentService` is the main orchestrator
- `org.personal.entity` — JPA entities: PaymentOrder, PaymentTransaction, QrCode, RefundOrder, PaymentEvent (audit log), Merchant, MasterMerchant
- `org.personal.repository` — Spring Data JPA repositories
- `org.personal.service.SimulatorClient` — HTTP client calling the simulator microservice

## Frontend Page Flow

```
MerchantPage (/)
  → enters merchantId, orderId, amount → stored in sessionStorage
CheckoutPage (/checkout)
  → displays summary, UPI VPA input → calls upiCollect → polls status
TransactionPage (/transaction/:id)
  → shows final SUCCESS/FAILED result
```

`ThreeDSPage` exists for Phase 2 (card payments, not yet active).

## Configuration

**Backend** (`backend/src/main/resources/application.properties`):
- DB: `jdbc:h2:file:./paymentdb` (file-based, persists between restarts)
- Simulator URL: `http://localhost:8181`
- QR expiry: 15 minutes; UPI callback validity: 15 minutes
- H2 console: `http://localhost:8080/h2-console` (user: `sa`, password: blank)

**Frontend** (`frontend/src/api.ts`):
- Backend base URL hardcoded as `http://localhost:8080`

## Test Data

**Seeded Merchants** (via `DataInitializer` on startup):

| Merchant ID | VPA |
|-------------|-----|
| MER001 | storea@axis |
| MER002 | storeb@hdfc |
| MER003 | storec@icici |

**Simulator Test VPAs:**

| VPA | Behavior |
|-----|----------|
| `success@upi` | Succeeds after ~1.5–2.5s callback |
| `fail@upi` | Payment fails |
| `timeout@upi` | No callback (stays PENDING) |
| `insufficient@upi` | Insufficient funds |
| `rbl.success@rbl`, `hdfc.success@hdfc`, `kotak.success@kotak` | Bank-specific success |

## Documentation
Comprehensive docs in `backend/docs/`:
- `DOMAIN_MODEL.md` — entity definitions and relationships
- `ER_DIAGRAM_AND_API.md` — full ER diagram and API spec with request/response examples
- `DATABASE_SCHEMA.md` — DDL, constraints, indexes
- `DEVELOPER_GUIDE.md` — layer patterns, state machine validation, testing patterns
- `WEBHOOK_GUIDE.md` — Phase 2 webhook design

See also `backend/CLAUDE.md` for backend-specific guidance.
