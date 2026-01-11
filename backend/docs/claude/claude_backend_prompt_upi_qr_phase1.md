# 🧠 Claude Backend Prompt  
## UPI QR Payment Gateway – Phase-1 (Java / Spring Boot)

---

## Role
You are a **Senior Backend Engineer** designing a **production-grade UPI QR Payment Gateway backend**.

This is **NOT a demo app**. Code quality, domain modeling, and extensibility matter.

---

## High-Level Objective
Build the **Phase-1 backend** of a UPI Payment Gateway that supports:

- Backend-generated UPI QR codes  
- Dynamic QR payments only (for now)  
- Multi-merchant model (Master Merchant → Child Merchants)  
- Polling-based payment status  
- On-read expiry evaluation (NO cron jobs)

---

## Tech Stack (Strict)
- Java 17+
- Spring Boot
- Spring Web (REST)
- Spring Data JPA
- H2 (in-memory)
- Maven
- OpenAPI / Swagger
- No security/authentication (deferred)

---

## Core Design Decisions (MANDATORY)

### 1️⃣ QR Generation
- QR **must be generated in backend**
- Frontend only renders Base64 / SVG QR
- QR embeds:
  - merchant VPA
  - amount
  - payment reference ID
  - expiry timestamp

---

### 2️⃣ Payment Expiry Strategy
- **On-read evaluation**
- No schedulers, no cron jobs
- Expiry is evaluated during:
  - `GET /payments/{paymentId}`

Logic:
```
IF now > expiresAt AND status == PENDING
→ mark payment as EXPIRED
```

Persist EXPIRED once.

---

### 3️⃣ Payment State Machine (STRICT)

```
CREATED
↓
QR_GENERATED
↓
PENDING
↓──────────────↓
SUCCESS        FAILED
↓
EXPIRED
```

Invalid transitions must throw errors.

---

## Domain Model (Phase-1 Scope)

### Entities
Use **UUID** as primary keys.

#### MasterMerchant
- id
- name
- status

#### Merchant
- id
- masterMerchantId
- name
- vpa
- status

#### PaymentOrder
- id
- merchantId
- amount
- currency
- status
- expiresAt
- createdAt

#### PaymentTransaction
- id
- paymentOrderId
- pspTxnId
- status
- createdAt

#### QrCode
- id
- paymentOrderId
- qrType (DYNAMIC)
- qrImageBase64
- upiIntent
- expiresAt

#### PaymentEvent
- id
- paymentOrderId
- eventType
- createdAt
- metadata (JSON/String)

---

## PSP Integration (Phase-1)

### Adapter Interface
```java
interface UpiPspAdapter {
    QrGenerationResult generateDynamicQr(QrRequest request);
    PaymentStatus fetchPaymentStatus(String pspTxnId);
}
```

### Implementation
- Implement `MockBankUpiAdapter`
- Simulate:
  - QR generation
  - Status transitions (PENDING → SUCCESS / FAILED)

No real HTTP calls.

---

## REST APIs (Phase-1)

### Merchant
```
POST /api/v1/merchants
```

---

### Payment
```
POST /api/v1/payments
GET  /api/v1/payments/{paymentId}
```

- `POST` creates payment + generates QR
- `GET`:
  - Evaluates expiry (on-read)
  - Returns current status

---

### QR
```
GET /api/v1/payments/{paymentId}/qr
```

Returns:
- Base64 QR
- UPI intent
- Expiry time

---

## Non-Functional Requirements
- Layered architecture (Controller → Service → Repository)
- DTOs only (no entity leakage)
- Centralized exception handling
- Clean logging
- Swagger UI enabled
- H2 Console enabled
- Auto schema via JPA
- README with run instructions
- Sample curl commands

---

## Package Structure (Must Follow)

```
com.upigateway
 ├── api
 ├── domain
 ├── service
 ├── repository
 ├── psp
 │   └── mockbank
 ├── exception
 ├── config
 └── util
```

---

## Deliverables
- Fully runnable Spring Boot project
- Clean, readable code
- Sensible defaults
- Extensible design (future PSPs, webhooks, auth)

---

## Coding Philosophy
- Clarity > Cleverness  
- Explicit state transitions  
- No shortcuts  
- No over-engineering  
- try to follow SOLID principles 

## Postman
at the end generate a Postman collection for the API.
