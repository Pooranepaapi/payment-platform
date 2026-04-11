# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Structure

| Project | Language | Port | Purpose |
|---------|----------|------|---------|
| `backend/` | Java 25, Spring Boot 4.0.2 | 8080 | Payment API, business logic, PostgreSQL |
| `frontend/` | TypeScript, React 19, Vite | 5173 | Checkout UI |
| `simulator/` | Java 25, Spring Boot 4.0.2 | 8181 | Bank PSP simulator (separate microservice) |

## Build & Run Commands

### Docker (recommended)
```bash
docker compose up --build       # Start all services (postgres, backend, simulator, frontend)
docker compose down             # Stop all
docker compose down -v          # Stop all and remove DB volume
```

### Local Development
```bash
# Backend (requires local PostgreSQL on port 5432)
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm install && npm run dev

# Simulator
cd simulator && mvn spring-boot:run

# Tests
cd backend && mvn test
cd backend && mvn test -Dtest=ClassName#methodName
```

## Architecture

### Request Flow
```
Frontend (5173) -> Backend (8080) -> Simulator (8181)
                                         | async callback (1.5-2.5s)
                   Backend (8080) <------'
```

### Backend Layers
```
@RestController -> @Service -> @Repository -> PostgreSQL
                      |
               SimulatorClient -> POST http://localhost:8181/api/simulator/upi/collect
```

### Payment Flow (Phase 1 - UPI)
1. `POST /api/v1/payments` -- create PaymentOrder (CREATED)
2. `POST /api/v1/payments/{id}/qr` -- generate QR code (QR_GENERATED)
3. `POST /api/payments/upi/collect` -- backend calls simulator (PENDING)
4. Simulator async callback -> `POST /api/transactions/upi/callback` (SUCCESS/FAILED)
5. Frontend polls `GET /api/v1/payments/{id}` until terminal state

### Payment Status Machine
`CREATED -> QR_GENERATED -> PENDING -> SUCCESS / FAILED / EXPIRED / CANCELLED`

### Amount Handling
All amounts stored as **paise** (Rs 1 = 100 paise, BIGINT). Never use floating point.

## Key Backend Packages

- `org.personal.controller` -- PaymentController, TransactionController
- `org.personal.service` -- PaymentService, PaymentOrderService, QrCodeService, SimulatorClient, PaymentExpiryJob
- `org.personal.entity` -- PaymentOrder, PaymentTransaction, QrCode, RefundOrder, PaymentEvent, Merchant, MasterMerchant
- `org.personal.repository` -- Spring Data JPA repositories

## Frontend Page Flow

```
MerchantPage (/) -> enters merchantId, orderId, amount -> sessionStorage
CheckoutPage (/checkout) -> UPI VPA input -> upiCollect -> polls status
TransactionPage (/transaction/:id) -> final SUCCESS/FAILED result
```

## Configuration

**Backend** (`application.properties`):
- DB: `jdbc:postgresql://localhost:5432/payment_gateway` (user: `postgres`, password: `postgres`)
- Simulator URL: `http://localhost:8181`
- QR expiry: 15 min, UPI callback validity: 15 min
- Docker overrides DB URL and simulator URL via environment variables in `docker-compose.yml`

**Frontend** (`src/api.ts`):
- Backend base URL: `http://localhost:8080`

## Documentation

- `docs/TEST_DATA.md` -- seeded merchants and simulator test VPAs
- `backend/docs/DOMAIN_MODEL.md` -- entity definitions and relationships
- `backend/docs/ER_DIAGRAM_AND_API.md` -- full ER diagram and API spec
- `backend/docs/DATABASE_SCHEMA.md` -- DDL, constraints, indexes
- `backend/docs/DEVELOPER_GUIDE.md` -- layer patterns, state machine, testing
- `backend/docs/WEBHOOK_GUIDE.md` -- Phase 2 webhook design
- `backend/CLAUDE.md` -- backend-specific development notes
