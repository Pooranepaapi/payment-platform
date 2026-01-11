# 🗄️ Database Schema - DDL Scripts

## Overview

- **Database**: H2 (Phase-1)
- **JPA Generation**: `spring.jpa.hibernate.ddl-auto=update`
- **Manual Tuning**: Yes (indexes, constraints)
- **Future Migration**: To MySQL/PostgreSQL (Phase-2)

---

## Entity Definitions (DDL)

### 1. master_merchant

```sql
CREATE TABLE master_merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    master_merchant_uuid VARCHAR(36) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE UNIQUE INDEX idx_master_uuid ON master_merchant(master_merchant_uuid);
```

---

### 2. merchant

```sql
CREATE TABLE merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_uuid VARCHAR(36) NOT NULL UNIQUE,
    master_merchant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    vpa VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    platform_fee_percentage DECIMAL(5, 2) NOT NULL DEFAULT 2.50,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_merchant_master
        FOREIGN KEY (master_merchant_id)
        REFERENCES master_merchant(id) ON DELETE RESTRICT,

    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CHECK (platform_fee_percentage >= 0 AND platform_fee_percentage <= 100)
);

CREATE UNIQUE INDEX idx_merchant_uuid ON merchant(merchant_uuid);
CREATE UNIQUE INDEX idx_merchant_vpa ON merchant(vpa);
CREATE INDEX idx_merchant_master ON merchant(master_merchant_id);
```

---

### 3. payment_order

```sql
CREATE TABLE payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_uuid VARCHAR(36) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    amount_in_paise BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    platform_fee_in_paise BIGINT NOT NULL,
    merchant_net_in_paise BIGINT NOT NULL,
    external_order_id VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_merchant
        FOREIGN KEY (merchant_id)
        REFERENCES merchant(id) ON DELETE RESTRICT,

    CHECK (status IN ('CREATED', 'QR_GENERATED', 'PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED')),
    CHECK (amount_in_paise > 0),
    CHECK (platform_fee_in_paise >= 0),
    CHECK (merchant_net_in_paise >= 0),
    CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX idx_payment_uuid ON payment_order(payment_uuid);
CREATE INDEX idx_payment_main ON payment_order(merchant_id, status, created_at);
CREATE INDEX idx_payment_expires ON payment_order(expires_at, status);
CREATE INDEX idx_payment_created ON payment_order(created_at DESC);
```

---

### 4. payment_transaction

```sql
CREATE TABLE payment_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_uuid VARCHAR(36) NOT NULL UNIQUE,
    payment_order_id BIGINT NOT NULL,
    psp_transaction_id VARCHAR(100) NOT NULL UNIQUE,
    psp_approval_code VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    failure_reason VARCHAR(500) NULL,
    psp_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_payment
        FOREIGN KEY (payment_order_id)
        REFERENCES payment_order(id) ON DELETE RESTRICT,

    CHECK (status IN ('INITIATED', 'PENDING', 'SUCCESS', 'FAILED'))
);

CREATE UNIQUE INDEX idx_psp_txn_id ON payment_transaction(psp_transaction_id);
CREATE INDEX idx_transaction_payment ON payment_transaction(payment_order_id);
CREATE INDEX idx_transaction_status ON payment_transaction(status);
```

---

### 5. qr_code

```sql
CREATE TABLE qr_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_order_id BIGINT NOT NULL UNIQUE,
    qr_type VARCHAR(50) NOT NULL DEFAULT 'DYNAMIC',
    qr_image_base64 LONGTEXT NOT NULL,
    qr_image_svg LONGTEXT NOT NULL,
    upi_intent VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_qr_payment
        FOREIGN KEY (payment_order_id)
        REFERENCES payment_order(id) ON DELETE CASCADE,

    CHECK (qr_type IN ('DYNAMIC'))
);

CREATE UNIQUE INDEX idx_qr_payment ON qr_code(payment_order_id);
```

---

### 6. payment_event

```sql
CREATE TABLE payment_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_order_id BIGINT NOT NULL,
    payment_uuid VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_payment
        FOREIGN KEY (payment_order_id)
        REFERENCES payment_order(id) ON DELETE RESTRICT,

    CHECK (event_type IN ('CREATED', 'QR_GENERATED', 'PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUND_INITIATED'))
);

CREATE INDEX idx_event_payment_time ON payment_event(payment_uuid, created_at);
CREATE INDEX idx_event_type ON payment_event(event_type);
```

---

### 7. refund_order

```sql
CREATE TABLE refund_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_uuid VARCHAR(36) NOT NULL UNIQUE,
    payment_order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    refund_amount_in_paise BIGINT NOT NULL,
    reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refund_payment
        FOREIGN KEY (payment_order_id)
        REFERENCES payment_order(id) ON DELETE RESTRICT,

    CHECK (status IN ('INITIATED', 'PENDING', 'SUCCESS', 'FAILED')),
    CHECK (refund_amount_in_paise > 0)
);

CREATE UNIQUE INDEX idx_refund_uuid ON refund_order(refund_uuid);
CREATE INDEX idx_refund_payment ON refund_order(payment_order_id);
```

---

## Enums (JPA)

```java
// MerchantStatus.java
public enum MerchantStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

// PaymentStatus.java
public enum PaymentStatus {
    CREATED, QR_GENERATED, PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED
}

// TransactionStatus.java
public enum TransactionStatus {
    INITIATED, PENDING, SUCCESS, FAILED
}

// PaymentEventType.java
public enum PaymentEventType {
    CREATED, QR_GENERATED, PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED, REFUND_INITIATED
}

// RefundStatus.java
public enum RefundStatus {
    INITIATED, PENDING, SUCCESS, FAILED
}

// QrType.java
public enum QrType {
    DYNAMIC
}
```

---

## Constraints & Validation

### NOT NULL Constraints

| Table | Field | Rule |
|---|---|---|
| payment_order | paymentUuid | External identifier |
| payment_order | amountInPaise | Must be present |
| payment_order | status | State machine |
| payment_order | expiresAt | Must be > createdAt |
| payment_transaction | pspTransactionId | Idempotency key |
| refund_order | refundAmountInPaise | Must be > 0 |

### UNIQUE Constraints

| Table | Field(s) | Reason |
|---|---|---|
| master_merchant | masterMerchantUuid | No duplicates |
| merchant | merchantUuid | No duplicates |
| merchant | vpa | UPI identity |
| payment_order | paymentUuid | External reference |
| payment_transaction | pspTransactionId | Idempotency |
| qr_code | paymentOrderId | 1:1 relationship |
| refund_order | refundUuid | Audit trail |

### FOREIGN KEY Constraints

| Table | Column | References | Action |
|---|---|---|---|
| merchant | masterMerchantId | master_merchant(id) | RESTRICT |
| payment_order | merchantId | merchant(id) | RESTRICT |
| payment_transaction | paymentOrderId | payment_order(id) | RESTRICT |
| qr_code | paymentOrderId | payment_order(id) | CASCADE |
| payment_event | paymentOrderId | payment_order(id) | RESTRICT |
| refund_order | paymentOrderId | payment_order(id) | RESTRICT |

**Note**: RESTRICT prevents accidental deletion of merchants/payments. CASCADE only on QrCode (child always deleted with payment).

---

## Indexes (Performance)

### Hot Query Indexes

```sql
-- List merchant's payments, filter by status, sort by date
CREATE INDEX idx_payment_main ON payment_order(merchant_id, status, created_at);

-- Find expired payments (on-read evaluation)
CREATE INDEX idx_payment_expires ON payment_order(expires_at, status);

-- Audit trail: what happened to this payment?
CREATE INDEX idx_event_payment_time ON payment_event(payment_uuid, created_at);

-- Idempotency: already processed this PSP transaction?
CREATE UNIQUE INDEX idx_psp_txn_id ON payment_transaction(psp_transaction_id);

-- Find payment by UUID (API GET)
CREATE UNIQUE INDEX idx_payment_uuid ON payment_order(payment_uuid);
```

### Index Strategy

- **Composite Index** `(merchant_id, status, created_at)`:
  - Merchant (partition)
  - Status (filter)
  - CreatedAt (sort)
  - Single index covers most queries

- **Unique Indexes**: UNIQUE constraints doubled as performance indexes

---

## Soft Delete Implementation

```sql
-- Find active merchants
SELECT * FROM merchant WHERE deleted_at IS NULL;

-- Find deleted merchants (audit)
SELECT * FROM merchant WHERE deleted_at IS NOT NULL;

-- Soft delete
UPDATE merchant SET deleted_at = NOW() WHERE id = ?;

-- Restore
UPDATE merchant SET deleted_at = NULL WHERE id = ?;
```

---

## Data Types Rationale

| Field | Type | Why |
|---|---|---|
| `id` | BIGINT | Auto-increment PK (8 bytes, efficient) |
| `*Uuid` | VARCHAR(36) | External identifier, unique index |
| `amountInPaise` | BIGINT | No floating-point errors |
| `currency` | VARCHAR(3) | ISO 4217 standard (e.g., INR, USD) |
| `status` | VARCHAR(50) | Enum stored as string (readable in DB) |
| `metadata` | JSON | Diagnostic data (optional, flexible) |
| `*At` | TIMESTAMP | Immutable timestamps |
| `deletedAt` | TIMESTAMP NULL | Soft delete marker |

---

## Sample Data

### MasterMerchant

```sql
INSERT INTO master_merchant (master_merchant_uuid, name, status)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'Payment Gateway Company Ltd', 'ACTIVE');
```

### Merchant (Children)

```sql
INSERT INTO merchant (merchant_uuid, master_merchant_id, name, vpa, platform_fee_percentage)
VALUES
  ('550e8400-e29b-41d4-a716-446655440002', 1, 'Test Store A', 'storea@axis', 2.50),
  ('550e8400-e29b-41d4-a716-446655440003', 1, 'Test Store B', 'storeb@hdfc', 2.50),
  ('550e8400-e29b-41d4-a716-446655440004', 1, 'Test Store C', 'storec@icici', 2.50);
```

### PaymentOrder

```sql
INSERT INTO payment_order
  (payment_uuid, merchant_id, status, amount_in_paise, currency,
   platform_fee_in_paise, merchant_net_in_paise, expires_at)
VALUES
  ('550e8400-e29b-41d4-a716-446655440005', 1, 'CREATED', 10050, 'INR', 250, 9800, DATE_ADD(NOW(), INTERVAL 15 MINUTE));
```

---

## Migration Strategy (H2 → MySQL/PostgreSQL)

### Phase-2 Considerations

1. **No Breaking Changes**: Schema already compatible
2. **Data Types**:
   - H2: LONGTEXT → MySQL: LONGTEXT
   - H2: JSON → MySQL: JSON
   - H2: TIMESTAMP → PostgreSQL: TIMESTAMP WITH TIME ZONE
3. **Indexes**: Copy as-is
4. **Constraints**: Copy as-is
5. **Enums**: Store as VARCHAR (not database enums)

---

## Query Examples (Common Patterns)

### Get Payment Status (with expiry check)

```sql
SELECT * FROM payment_order WHERE payment_uuid = ? FOR UPDATE;

-- Application checks:
-- IF now() > expires_at AND status = 'PENDING'
--   THEN UPDATE status = 'EXPIRED'
```

### List Merchant's Payments (paginated)

```sql
SELECT * FROM payment_order
WHERE merchant_id = ? AND status = 'SUCCESS'
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- Uses: idx_payment_main(merchant_id, status, created_at)
```

### Audit Trail (complete history)

```sql
SELECT * FROM payment_event
WHERE payment_uuid = ?
ORDER BY created_at ASC;

-- Uses: idx_event_payment_time(payment_uuid, created_at)
```

### Idempotency (prevent duplicate processing)

```sql
SELECT * FROM payment_transaction
WHERE psp_transaction_id = ?;

-- Uses: idx_psp_txn_id (UNIQUE)
```

---

## Performance Expectations

| Query | Index | Estimated Time |
|---|---|---|
| Get payment by UUID | idx_payment_uuid (UNIQUE) | < 1ms |
| List merchant payments | idx_payment_main | < 10ms |
| Check PSP idempotency | idx_psp_txn_id (UNIQUE) | < 1ms |
| Audit trail | idx_event_payment_time | < 10ms |
| Find expired payments | idx_payment_expires | < 100ms |

(With 1M records in payment_order, H2 in-memory)

---

This schema is **production-ready for Phase-1** and **scalable to Phase-2+** without structural changes.
