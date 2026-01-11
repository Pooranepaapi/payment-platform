# 🧱 Domain Model - UPI QR Payment Gateway

## Overview

**Scope**: Payment Core Only
**Responsibility**: "Did the customer pay or not?"
**Scale**: Phase-1 (1,000 payments/day)

---

## Entity Definitions

### 1. MasterMerchant

**Purpose**: Top-level organization (e.g., "Payment Gateway Company Ltd")

```java
@Entity
@Table(name = "master_merchant")
public class MasterMerchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String masterMerchantUuid; // UUID as String

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status; // ACTIVE, INACTIVE, SUSPENDED

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime deletedAt; // Soft delete

    // Relationships
    @OneToMany(mappedBy = "masterMerchant", cascade = CascadeType.NONE)
    private List<Merchant> children = new ArrayList<>();
}
```

---

### 2. Merchant (Child Merchant)

**Purpose**: Actual store/business that receives payments

```java
@Entity
@Table(name = "merchant")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String merchantUuid; // UUID as String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_merchant_id", nullable = false)
    private MasterMerchant masterMerchant;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String vpa; // Unique UPI VPA (e.g., storea@axis)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status; // ACTIVE, INACTIVE, SUSPENDED

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal platformFeePercentage; // e.g., 2.50 (%)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime deletedAt; // Soft delete

    // Relationships
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.NONE)
    private List<PaymentOrder> payments = new ArrayList<>();
}

// Enum: MerchantStatus
public enum MerchantStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
```

---

### 3. PaymentOrder

**Purpose**: Core payment entity (immutable snapshot)

```java
@Entity
@Table(name = "payment_order", indexes = {
    @Index(name = "idx_payment_main", columnList = "merchant_id,status,created_at"),
    @Index(name = "idx_payment_uuid", columnList = "payment_uuid", unique = true)
})
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String paymentUuid; // UUID for external reference

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; // CREATED, QR_GENERATED, PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED

    // Amount in paise (multiply by 100 to get display amount)
    @Column(nullable = false)
    private Long amountInPaise; // ₹100.50 = 10050 paise

    @Column(nullable = false, length = 3)
    private String currency; // INR, USD, EUR, etc.

    // Fees (immutable snapshot at time of payment)
    @Column(nullable = false)
    private Long platformFeeInPaise; // Calculated at payment time

    @Column(nullable = false)
    private Long merchantNetInPaise; // amount - platformFee

    @Column(nullable = true, length = 255)
    private String externalOrderId; // Merchant's reference

    @Column(nullable = true, length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDateTime expiresAt; // QR expiry (now + 15 min)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToOne(mappedBy = "paymentOrder", cascade = CascadeType.NONE)
    private QrCode qrCode;

    @OneToMany(mappedBy = "paymentOrder", cascade = CascadeType.NONE)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "paymentOrder", cascade = CascadeType.NONE)
    private List<PaymentEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "paymentOrder", cascade = CascadeType.NONE)
    private List<RefundOrder> refunds = new ArrayList<>();
}

// Enum: PaymentStatus
public enum PaymentStatus {
    CREATED,          // Payment order created
    QR_GENERATED,     // QR image generated
    PENDING,          // Awaiting customer approval
    SUCCESS,          // Payment successful
    FAILED,           // Payment failed
    EXPIRED,          // QR expired before approval
    CANCELLED         // Cancelled before PENDING
}
```

---

### 4. PaymentTransaction

**Purpose**: PSP interaction record (one per payment attempt)

```java
@Entity
@Table(name = "payment_transaction", indexes = {
    @Index(name = "idx_psp_txn_id", columnList = "psp_transaction_id", unique = true)
})
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String transactionUuid; // UUID for audit trail

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Column(nullable = false, length = 100)
    private String pspTransactionId; // From PSP (for idempotency)

    @Column(nullable = true, length = 50)
    private String pspApprovalCode; // If approved

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; // INITIATED, SUCCESS, FAILED, PENDING

    @Column(nullable = true, length = 500)
    private String failureReason;

    @Column(nullable = false, length = 50)
    private String pspName; // e.g., "AXIS", "HDFC"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

// Enum: TransactionStatus
public enum TransactionStatus {
    INITIATED,    // PSP received request
    PENDING,      // Awaiting PSP response
    SUCCESS,      // PSP confirmed success
    FAILED        // PSP confirmed failure
}
```

---

### 5. QrCode

**Purpose**: QR image metadata (linked 1:1 with PaymentOrder)

```java
@Entity
@Table(name = "qr_code", indexes = {
    @Index(name = "idx_qr_payment", columnList = "payment_order_id", unique = true)
})
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false, unique = true)
    private PaymentOrder paymentOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QrType qrType; // DYNAMIC (static not in Phase-1)

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String qrImageBase64; // Base64-encoded PNG

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String qrImageSvg; // SVG for scaling

    @Column(nullable = false, length = 500)
    private String upiIntent; // upi://pay?pa=...&am=...&tn=...

    @Column(nullable = false)
    private LocalDateTime expiresAt; // Same as PaymentOrder

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

// Enum: QrType
public enum QrType {
    DYNAMIC  // Phase-1: only dynamic QR
}
```

---

### 6. PaymentEvent

**Purpose**: Immutable audit trail (event sourcing lite)

```java
@Entity
@Table(name = "payment_event", indexes = {
    @Index(name = "idx_event_payment_time", columnList = "payment_uuid,created_at")
})
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Column(nullable = false, length = 36)
    private String paymentUuid; // Denormalized for faster queries

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentEventType eventType;

    @Column(nullable = true, columnDefinition = "JSON")
    private String metadata; // Non-PII diagnostic data

    // Example metadata:
    // {
    //   "pspTransactionId": "PSP_12345",
    //   "pspApprovalCode": "ABC123",
    //   "psp": "AXIS",
    //   "approvalTime": "2026-01-10T10:05:30Z",
    //   "source": "UPI_APP"
    // }

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

// Enum: PaymentEventType
public enum PaymentEventType {
    CREATED,           // PaymentOrder created
    QR_GENERATED,      // QR image generated
    PENDING,           // Awaiting approval
    SUCCESS,           // Payment successful
    FAILED,            // Payment failed
    EXPIRED,           // QR expired
    CANCELLED,         // Payment cancelled
    REFUND_INITIATED   // Refund started
}
```

---

### 7. RefundOrder

**Purpose**: Refund request & lifecycle (supports partial + multiple)

```java
@Entity
@Table(name = "refund_order")
public class RefundOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String refundUuid; // UUID for tracking

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status; // INITIATED, PENDING, SUCCESS, FAILED

    // Amount in paise
    @Column(nullable = false)
    private Long refundAmountInPaise; // Can be partial

    @Column(nullable = true, length = 255)
    private String reason; // Why refund?

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

// Enum: RefundStatus
public enum RefundStatus {
    INITIATED,  // Refund requested
    PENDING,    // Waiting for PSP
    SUCCESS,    // Refund completed
    FAILED      // Refund failed
}
```

---

## Entity Relationships Summary

```
MasterMerchant (1)
  └── Merchant (Many)
        └── PaymentOrder (Many)
              ├── QrCode (1:1)
              ├── PaymentTransaction (Many)
              ├── PaymentEvent (Many)
              └── RefundOrder (Many)
```

---

## Key Design Decisions

### ✅ Why Long PKs + UUID Fields?

- **Performance**: Long PKs (8 bytes) vs UUID PKs (16 bytes)
- **Indexing**: Faster B-tree operations with Long
- **External Sharing**: UUID fields for APIs
- **Migration-friendly**: Can change UUID strategy later
- **Reconciliation**: Long IDs stable internally, UUID for external

### ✅ Why Amounts in Paise (BIGINT)?

- **Precision**: No floating-point rounding errors
- **Reconciliation**: Exact penny-matching
- **Industry Standard**: All payment systems use this
- **Consistency**: ₹100.50 = 10050 paise (no confusion)

### ✅ Why Soft Deletes?

- **Audit Trail**: Never lose financial records
- **Reconciliation**: Can trace deleted merchants
- **Legal**: Tax/regulatory requirements
- **Un-delete**: Can restore accidentally deleted data

### ✅ Why Composite Index (merchant, status, createdAt)?

- **Most Common Queries**: List merchant's payments, filter by status, sort by date
- **Index Ordering**: Merchant (partition) → Status (filter) → CreatedAt (sort)
- **Performance**: Single index covers multiple predicates

### ✅ Why Separate PaymentEvent Table?

- **Immutable**: Events never change (append-only log)
- **Audit**: Complete history of every state change
- **Future**: Phase-2 webhooks emit from this table
- **Compliance**: Non-repudiation proof

### ✅ Why RefundOrder Separate?

- **Lifecycle**: Refunds have independent state
- **Partial Refunds**: One payment → many refunds
- **Tracking**: Each refund has UUID for idempotency
- **Future**: Settlement, tax reporting

---

## Field Constraints & Validation

### PaymentOrder

| Field | Type | Constraint | Rule |
|---|---|---|---|
| `paymentUuid` | VARCHAR(36) | UNIQUE, NOT NULL | Must be UUID |
| `amountInPaise` | BIGINT | NOT NULL | Must be > 0 |
| `currency` | VARCHAR(3) | NOT NULL | Must be valid ISO 4217 |
| `status` | ENUM | NOT NULL | State machine enforced |
| `expiresAt` | DATETIME | NOT NULL | Must be > createdAt |
| `deletedAt` | DATETIME | NULL | Soft delete marker |

### Merchant

| Field | Type | Constraint | Rule |
|---|---|---|---|
| `vpa` | VARCHAR(50) | UNIQUE, NOT NULL | Must be valid UPI VPA format |
| `platformFeePercentage` | DECIMAL(5,2) | NOT NULL | Must be 0 ≤ x ≤ 100 |
| `status` | ENUM | NOT NULL | Only active merchants receive payments |

### PaymentTransaction

| Field | Type | Constraint | Rule |
|---|---|---|---|
| `pspTransactionId` | VARCHAR(100) | UNIQUE, NOT NULL | Idempotency key |
| `status` | ENUM | NOT NULL | Represents PSP response |

---

## State Transitions (Explicit)

### PaymentOrder Status

```
CREATED
  → QR_GENERATED (backend generates QR)
  → CANCELLED (only from CREATED/QR_GENERATED)

QR_GENERATED
  → PENDING (customer scans & approves)
  → EXPIRED (on-read, if now > expiresAt)
  → CANCELLED (merchant cancels)

PENDING
  → SUCCESS (PSP confirms)
  → FAILED (PSP declines)
  → EXPIRED (on-read, if now > expiresAt & !SUCCESS/FAILED)

SUCCESS
  → (TERMINAL - no further changes)

FAILED
  → (TERMINAL - customer must create new payment)

EXPIRED
  → (TERMINAL - customer must create new payment)

CANCELLED
  → (TERMINAL - customer must create new payment)
```

### PaymentTransaction Status

```
INITIATED
  → PENDING (waiting for PSP response)

PENDING
  → SUCCESS (payment approved)
  → FAILED (payment declined)

SUCCESS / FAILED
  → (TERMINAL)
```

### RefundOrder Status

```
INITIATED
  → PENDING (sent to PSP)

PENDING
  → SUCCESS (PSP confirmed)
  → FAILED (PSP declined)

SUCCESS / FAILED
  → (TERMINAL)
```

---

## Audit & Compliance

### What's Tracked (PaymentEvent)

✅ Status changes (CREATED → QR_GENERATED → SUCCESS)
✅ Timestamps (exact moment of each event)
✅ PSP references (for reconciliation)
✅ Error codes (for debugging)

### What's NOT Tracked (Intentional)

❌ Customer phone
❌ Customer UPI ID
❌ Customer name
❌ Access logs (Phase-2)

**Rationale**: Payment system is not a wallet/CRM. Customer identity is PSP's responsibility.

---

## Future Extensions (Phase-2+)

- **Settlement**: Add `settlementStatus`, `settledAt` (migrations, not schema change)
- **Webhooks**: Separate `WebhookDelivery` table (append-only)
- **Analytics**: Denormalized summary tables (separate OLAP DB)
- **Payouts**: `PayoutOrder` entity (linked to settlements)
- **Card Payments**: `CardPaymentOrder` (new payment method)

**Design Principle**: Schema must support these without major rework.

---

## Indexes (Complete)

```sql
-- PaymentOrder
CREATE UNIQUE INDEX idx_payment_uuid ON payment_order(payment_uuid);
CREATE INDEX idx_payment_main ON payment_order(merchant_id, status, created_at);

-- PaymentTransaction
CREATE UNIQUE INDEX idx_psp_txn_id ON payment_transaction(psp_transaction_id);

-- QrCode
CREATE UNIQUE INDEX idx_qr_payment ON qr_code(payment_order_id);

-- PaymentEvent
CREATE INDEX idx_event_payment_time ON payment_event(payment_uuid, created_at);

-- Merchant
CREATE UNIQUE INDEX idx_merchant_uuid ON merchant(merchant_uuid);
CREATE UNIQUE INDEX idx_merchant_vpa ON merchant(vpa);

-- MasterMerchant
CREATE UNIQUE INDEX idx_master_uuid ON master_merchant(master_merchant_uuid);
```

---

This domain model is **complete for Phase-1** and **extensible for Phase-2+**. No breaking changes required.
