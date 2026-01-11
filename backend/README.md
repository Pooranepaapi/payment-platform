# 💳 UPI QR Payment Gateway - Phase-1

A production-grade payment processing backend for **UPI QR code payments**. Backend-generated QR codes, real-time polling, and complete audit trail.

---

## 🚀 Quick Start

### Prerequisites
- **Java 11+** (JDK)
- **Maven 3.6+**

### Run the Backend

```bash
# Clone/Navigate to project
cd payment-backend

# Build
mvn clean compile

# Run (starts on http://localhost:8080)
mvn spring-boot:run
```

The application will:
- ✅ Start embedded H2 database
- ✅ Auto-create schema (JPA DDL mode: `update`)
- ✅ Seed test merchants and contracts
- ✅ Ready to accept requests

---

## 🧪 Test the System

### 1. Create a Payment Order

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "MER001",
    "amount": 250.00,
    "description": "Test Payment"
  }'
```

**Response:**
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440001",
  "amount": 250.00,
  "status": "CREATED",
  "expiresAt": "2026-01-10T10:30:00Z"
}
```

### 2. Generate QR Code

```bash
curl http://localhost:8080/api/v1/payments/{paymentId}/qr
```

**Response:**
```json
{
  "qrImageBase64": "data:image/png;base64,...",
  "upiIntent": "upi://pay?pa=...",
  "expiresAt": "2026-01-10T10:30:00Z"
}
```

### 3. Poll Payment Status

```bash
curl http://localhost:8080/api/v1/payments/{paymentId}
```

---

## 📊 Test Merchants

| Merchant ID | Name | VPA | Fee |
|---|---|---|---|
| `MER001` | Test Store A | storea@axis | 2.50% |
| `MER002` | Test Store B | storeb@hdfc | 2.50% |
| `MER003` | Test Store C | storec@icici | 2.50% |

---

## 📁 Project Structure

```
payment-backend/
├── src/main/java/org/personal/
│   ├── entity/              # JPA entities (7 tables)
│   ├── repository/          # Spring Data repositories
│   ├── service/             # Business logic & PSP integration
│   ├── controller/          # REST API endpoints
│   ├── dto/                 # Request/Response objects
│   ├── exception/           # Custom exceptions
│   ├── config/              # Spring config (Clock, Jackson)
│   └── util/                # Utilities (UUID, Amount conversion)
├── src/main/resources/
│   ├── application.properties
│   └── data.sql             # Test data
├── src/test/java/           # Integration tests
├── docs/                    # Documentation
│   ├── DATABASE_SCHEMA.md   # DDL, constraints, indexes
│   ├── DOMAIN_MODEL.md      # Entity definitions & design
│   ├── ER_DIAGRAM_AND_API.md# API specification
│   ├── DEVELOPER_GUIDE.md   # Architecture patterns, tips
│   ├── IMPLEMENTATION_PLAN.md # Development roadmap
│   └── WEBHOOK_GUIDE.md     # Webhook integration (Phase-2)
└── README.md                # This file
```

---

## 🔗 Documentation

All detailed documentation is in the `docs/` folder.

### For Product & Operations
- **This README** - Quick start & test credentials

### For Architects & DBAs
- **[DOMAIN_MODEL.md](./docs/DOMAIN_MODEL.md)** - Entity definitions, relationships, state machines
- **[DATABASE_SCHEMA.md](./docs/DATABASE_SCHEMA.md)** - DDL scripts, constraints, indexes

### For Developers & Implementers
- **[DEVELOPER_GUIDE.md](./docs/DEVELOPER_GUIDE.md)** - Architecture patterns, common workflows
- **[ER_DIAGRAM_AND_API.md](./docs/ER_DIAGRAM_AND_API.md)** - Complete API specification
- **[IMPLEMENTATION_PLAN.md](./docs/IMPLEMENTATION_PLAN.md)** - Development roadmap

---

## 🏗️ Architecture Overview

```
HTTP Request
    ↓
Controller (@PostMapping, @GetMapping)
    ↓
Service (Business Logic, State Validation)
    ↓
Repository (JPA Queries)
    ↓
H2 Database (OLTP, Strongly Consistent)
```

### Key Entities
- **MasterMerchant** - Organization (e.g., "Payment Gateway Ltd")
- **Merchant** - Individual store (e.g., "ABC Store")
- **PaymentOrder** - Payment request with QR code
- **PaymentTransaction** - PSP interaction record (one per attempt)
- **QrCode** - Generated QR image + UPI intent
- **PaymentEvent** - Immutable audit trail (append-only log)
- **RefundOrder** - Refund request & lifecycle

**Learn more**: See [DOMAIN_MODEL.md](./docs/DOMAIN_MODEL.md#entity-relationships-summary)

---

## 💰 Payment Flow

```
1. POST /api/v1/payments
   → Create PaymentOrder (status: CREATED)
   → Calculate fees & net amount
   → Return paymentId

2. GET /api/v1/payments/{paymentId}/qr
   → Generate QR code (Base64 + SVG + UPI intent)
   → Return qrImageBase64

3. GET /api/v1/payments/{paymentId} (poll every 5s)
   → Check status from PaymentTransaction
   → Evaluate expiry (on-read, not in cron)
   → Return PENDING / SUCCESS / FAILED / EXPIRED

4. On SUCCESS
   → PaymentOrder status = SUCCESS
   → PaymentEvent created (audit trail)
   → Ready for settlement (Phase-2)
```

**Learn more**: See [ER_DIAGRAM_AND_API.md](./docs/ER_DIAGRAM_AND_API.md)

---

## 🧮 Amount Handling

All amounts stored as **paise** (₹1 = 100 paise):
- Input: `₹250.00` (rupees)
- Stored: `25000` (paise, BIGINT)
- No floating-point errors ✅

**Example:**
```json
{
  "amount": 250.00,           // Frontend input
  "amountInPaise": 25000,     // Database storage
  "platformFeePercentage": 2.50,
  "platformFeeInPaise": 625,  // Calculated: 25000 * 2.5 / 100
  "merchantNetInPaise": 24375 // Amount - fee
}
```

---

## 🔐 Idempotency & Webhooks

### Payment UUID
Every payment gets a unique UUID (`paymentId`):
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440001",  // Share with customer
  "status": "PENDING"
}
```

✅ Use for customer payment links
✅ Share in webhooks (Phase-2)
✅ Reference in refunds

**Learn more**: See [WEBHOOK_GUIDE.md](./docs/WEBHOOK_GUIDE.md)

---

## 🧪 H2 Database Console

Access in-memory DB via browser:

```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./paymentdb
User: sa
Password: (leave blank)
```

View live tables, run queries, inspect payment data.

---

## 📋 Requirements

### Phase-1 (Complete)
✅ Core payment processing
✅ QR code generation (Backend)
✅ Real-time polling
✅ Audit trail (PaymentEvent table)
✅ Multi-merchant support
✅ Partial & multiple refunds

### Phase-2 (Future)
🔄 Webhooks (outbound events)
🔄 Settlement & payout
🔄 Card payment support

### Phase-3 (Future)
📊 Dashboards & analytics
🔒 Cold storage for compliance

---

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:h2:file:./paymentdb
spring.jpa.hibernate.ddl-auto=update

# Server
server.port=8080

# Logging
logging.level.root=INFO
logging.level.org.personal=DEBUG
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|---|---|
| **Port 8080 already in use** | Change `server.port` in application.properties |
| **Schema mismatch errors** | Delete `paymentdb.mv.db` file, restart (auto-recreates schema) |
| **Merchant not found** | Ensure merchant ID matches test data (MER001, MER002, MER003) |
| **QR not generating** | Check backend logs for ZXing errors, verify paymentId exists |

**More help**: See [DEVELOPER_GUIDE.md](./docs/DEVELOPER_GUIDE.md#troubleshooting)

---

## 📞 Support

- **Architecture Questions** → See [DOMAIN_MODEL.md](./docs/DOMAIN_MODEL.md)
- **API Questions** → See [ER_DIAGRAM_AND_API.md](./docs/ER_DIAGRAM_AND_API.md)
- **Development Help** → See [DEVELOPER_GUIDE.md](./docs/DEVELOPER_GUIDE.md)
- **Database Tuning** → See [DATABASE_SCHEMA.md](./docs/DATABASE_SCHEMA.md#performance-expectations)

---

## 📄 License

Internal Use Only

---

**Last Updated**: 2026-01-10
**Version**: Phase-1.0
**Backend Port**: 8080
