# 🛠️ Developer Guide - UPI QR Payment Gateway

Comprehensive guide for developers implementing features, extending the system, and debugging issues.

---

## Table of Contents

1. [Architecture Patterns](#architecture-patterns)
2. [Common Workflows](#common-workflows)
3. [Adding a New Endpoint](#adding-a-new-endpoint)
4. [State Machine Validation](#state-machine-validation)
5. [Testing Patterns](#testing-patterns)
6. [Performance Tuning](#performance-tuning)
7. [Common Mistakes](#common-mistakes)
8. [Troubleshooting](#troubleshooting)

---

## Architecture Patterns

### Layer 1: Controller (`@RestController`)

**Responsibility**: HTTP handling, request validation, response formatting

```java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @RequestBody CreatePaymentRequest request,
        @RequestParam String merchantId) {

        // 1. Validate input (length, format, enum values)
        if (request.getAmount() <= 0) {
            throw new BadRequestException("Amount must be > 0");
        }

        // 2. Call service (business logic)
        Payment payment = paymentService.createPayment(
            merchantId,
            request.getAmount()
        );

        // 3. Map entity to DTO
        PaymentResponse response = new PaymentResponse(
            payment.getPaymentUuid(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getExpiresAt()
        );

        // 4. Return with appropriate status code
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

**Key Points:**
- ✅ Never call Repository directly from Controller
- ✅ Always validate input (not null, right format)
- ✅ Map entities to DTOs (don't expose JPA entities)
- ✅ Return proper HTTP status codes (201 for POST, 200 for GET, 404 for not found)

---

### Layer 2: Service (`@Service`, `@Transactional`)

**Responsibility**: Business logic, state validation, orchestration

```java
@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private Clock clock;  // Mockable for testing

    public Payment createPayment(String merchantId, BigDecimal amount) {
        // 1. Validate preconditions
        Merchant merchant = merchantRepository.findByMerchantUuid(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

        if (!merchant.getStatus().equals(MerchantStatus.ACTIVE)) {
            throw new BusinessException("Merchant is not active");
        }

        // 2. Calculate derived fields (immutable snapshot)
        long amountInPaise = convertToPaise(amount);
        long platformFeeInPaise = calculatePlatformFee(
            amountInPaise,
            merchant.getPlatformFeePercentage()
        );
        long merchantNetInPaise = amountInPaise - platformFeeInPaise;

        // 3. Create entity
        Payment payment = new Payment();
        payment.setPaymentUuid(UUID.randomUUID().toString());
        payment.setMerchant(merchant);
        payment.setAmountInPaise(amountInPaise);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setExpiresAt(Instant.now(clock).plus(15, ChronoUnit.MINUTES));
        payment.setPlatformFeeInPaise(platformFeeInPaise);
        payment.setMerchantNetInPaise(merchantNetInPaise);
        payment.setCreatedAt(Instant.now(clock));
        payment.setUpdatedAt(Instant.now(clock));

        // 4. Persist
        Payment savedPayment = paymentRepository.save(payment);

        // 5. Record event (audit trail)
        paymentEventRepository.save(createEvent(
            savedPayment,
            PaymentEventType.CREATED
        ));

        return savedPayment;
    }
}
```

**Key Points:**
- ✅ Inject dependencies via `@Autowired`
- ✅ Mark with `@Transactional` for automatic rollback on exception
- ✅ Validate preconditions (merchant exists, is active, etc.)
- ✅ Use `Clock` bean for testable timestamps (not `System.currentTimeMillis()`)
- ✅ Calculate immutable snapshots at creation time (fees, net amount)
- ✅ Record events for audit trail
- ✅ Return entities (DTO conversion happens in Controller)

---

### Layer 3: Repository (`extends JpaRepository`)

**Responsibility**: Data access, query abstraction

```java
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by UUID (external reference)
    Optional<Payment> findByPaymentUuid(String paymentUuid);

    // List merchant's payments (composite index: merchant_id, status, created_at)
    @Query("""
        SELECT p FROM Payment p
        WHERE p.merchant.id = :merchantId
        AND p.status = :status
        ORDER BY p.createdAt DESC
    """)
    Page<Payment> findByMerchantAndStatus(
        Long merchantId,
        PaymentStatus status,
        Pageable pageable
    );

    // Find expired payments
    @Query("""
        SELECT p FROM Payment p
        WHERE p.status IN ('PENDING', 'QR_GENERATED')
        AND p.expiresAt < CURRENT_TIMESTAMP
    """)
    List<Payment> findExpiredPayments();
}
```

**Key Points:**
- ✅ Use `Optional<T>` for nullable results (not null checks)
- ✅ Use `@Query` for complex queries
- ✅ Use `Page<T>` for paginated results
- ✅ Method names should describe intent (e.g., `findByPaymentUuid`, not `getPaymentByUuid`)

---

### Layer 4: Entity (`@Entity`)

**Responsibility**: JPA mapping, persistence metadata

```java
@Entity
@Table(name = "payment_order")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String paymentUuid;  // External reference (UUID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Long amountInPaise;  // ₹100.50 = 10050

    @Column(nullable = false)
    private String currency;  // "INR", "USD"

    @Column(nullable = false)
    private Long platformFeeInPaise;  // Immutable snapshot

    @Column(nullable = false)
    private Long merchantNetInPaise;  // Immutable snapshot

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // Getters & setters
    // ...
}
```

**Key Points:**
- ✅ Use `@Enumerated(EnumType.STRING)` (not ORDINAL) for enums
- ✅ Use `@Lazy` for relationships (don't load unless accessed)
- ✅ Use `@GeneratedValue(IDENTITY)` for auto-increment PKs
- ✅ Use `BIGINT` for amounts (paise, not rupees)
- ✅ Use `Instant` for timestamps (UTC, not LocalDateTime)
- ✅ Mark as `updatable = false` for immutable fields (createdAt, uuid)

---

## Common Workflows

### Workflow 1: Validating Payment State

**Scenario**: Before capturing a payment, ensure it's in authorized state.

```java
public void validatePaymentState(Payment payment, PaymentStatus expectedStatus) {
    if (!payment.getStatus().equals(expectedStatus)) {
        throw new InvalidStateException(
            String.format(
                "Payment is in %s state, expected %s",
                payment.getStatus(),
                expectedStatus
            )
        );
    }
}

// Usage
Payment payment = paymentRepository.findByPaymentUuid(paymentId)
    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

validatePaymentState(payment, PaymentStatus.PENDING);
```

### Workflow 2: Transitioning Payment Status

**Scenario**: Move payment from PENDING → SUCCESS after PSP confirmation.

```java
@Transactional
public void markPaymentSuccess(String paymentUuid, String pspTransactionId) {
    // 1. Load payment (with lock for consistency)
    Payment payment = paymentRepository.findByPaymentUuid(paymentUuid)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

    // 2. Validate precondition
    if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
        throw new InvalidStateException("Payment is not in PENDING state");
    }

    // 3. Update status
    payment.setStatus(PaymentStatus.SUCCESS);
    payment.setUpdatedAt(Instant.now(clock));
    paymentRepository.save(payment);

    // 4. Record event (audit trail)
    paymentEventRepository.save(new PaymentEvent(
        payment,
        payment.getPaymentUuid(),
        PaymentEventType.SUCCESS,
        Map.of(
            "pspTransactionId", pspTransactionId,
            "succeededAt", Instant.now(clock).toString()
        )
    ));

    // 5. Handle downstream (settlement, webhooks, etc. in Phase-2)
}
```

**Key Points:**
- ✅ Always validate precondition before state change
- ✅ Update timestamp when changing state
- ✅ Record event immediately (immutable audit trail)
- ✅ Use `@Transactional` for atomicity

### Workflow 3: Calculating Fees

**Scenario**: Platform takes 2.5%, merchant gets remainder.

```java
public long calculatePlatformFee(
    long amountInPaise,
    BigDecimal feePercentage) {

    // Math: 10050 paise * 2.5% = 251.25 paise → round to 251
    BigDecimal feeAmount = BigDecimal.valueOf(amountInPaise)
        .multiply(feePercentage)
        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

    return feeAmount.longValue();
}

// Test case
long fee = calculatePlatformFee(10050, BigDecimal.valueOf(2.5));
assert fee == 251;  // 10050 * 2.5 / 100 = 251.25 → 251
```

**Key Points:**
- ✅ Use `BigDecimal` for money calculations (not `double`)
- ✅ Use `RoundingMode.HALF_UP` for standard rounding
- ✅ Result is always BIGINT (paise)

---

## Adding a New Endpoint

**Task**: Add endpoint to refund a payment

### Step 1: Create Request/Response DTOs

```java
// RefundPaymentRequest.java
public class RefundPaymentRequest {
    private Long refundAmountInPaise;  // Optional (null = full refund)
    private String reason;              // Optional

    // Getters & setters
}

// RefundPaymentResponse.java
public class RefundPaymentResponse {
    private String refundId;
    private String status;  // INITIATED, PENDING, SUCCESS, FAILED
    private Long refundAmountInPaise;

    public RefundPaymentResponse(RefundOrder refund) {
        this.refundId = refund.getRefundUuid();
        this.status = refund.getStatus().toString();
        this.refundAmountInPaise = refund.getRefundAmountInPaise();
    }
}
```

### Step 2: Create Service Method

```java
@Service
@Transactional
public class RefundService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundOrderRepository refundOrderRepository;

    @Autowired
    private Clock clock;

    public RefundOrder refundPayment(
        String paymentUuid,
        Long refundAmountInPaise,
        String reason) {

        // 1. Load payment
        Payment payment = paymentRepository.findByPaymentUuid(paymentUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // 2. Validate precondition (payment must be successful)
        if (!payment.getStatus().equals(PaymentStatus.SUCCESS)) {
            throw new InvalidStateException("Only successful payments can be refunded");
        }

        // 3. Determine refund amount (default: full refund)
        long actualRefundAmount = refundAmountInPaise != null
            ? refundAmountInPaise
            : payment.getAmountInPaise();

        // 4. Validate refund amount
        if (actualRefundAmount <= 0 || actualRefundAmount > payment.getAmountInPaise()) {
            throw new BadRequestException(
                "Refund amount must be > 0 and <= " + payment.getAmountInPaise()
            );
        }

        // 5. Create RefundOrder
        RefundOrder refund = new RefundOrder();
        refund.setRefundUuid(UUID.randomUUID().toString());
        refund.setPayment(payment);
        refund.setRefundAmountInPaise(actualRefundAmount);
        refund.setReason(reason);
        refund.setStatus(RefundStatus.INITIATED);
        refund.setCreatedAt(Instant.now(clock));
        refund.setUpdatedAt(Instant.now(clock));

        RefundOrder savedRefund = refundOrderRepository.save(refund);

        // 6. Record event
        paymentEventRepository.save(new PaymentEvent(
            payment,
            payment.getPaymentUuid(),
            PaymentEventType.REFUND_INITIATED,
            Map.of(
                "refundId", savedRefund.getRefundUuid(),
                "refundAmount", actualRefundAmount,
                "reason", reason != null ? reason : ""
            )
        ));

        // 7. Send to PSP (Phase-2)
        // pspAdapterService.initiateRefund(savedRefund);

        return savedRefund;
    }
}
```

### Step 3: Create Controller Endpoint

```java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private RefundService refundService;

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundPaymentResponse> refundPayment(
        @PathVariable String paymentId,
        @RequestBody RefundPaymentRequest request) {

        RefundOrder refund = refundService.refundPayment(
            paymentId,
            request.getRefundAmountInPaise(),
            request.getReason()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RefundPaymentResponse(refund));
    }
}
```

### Step 4: Create Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
public class RefundPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    public void testRefundSuccessfulPayment() throws Exception {
        // 1. Create a successful payment
        Payment payment = createSuccessfulPayment("MER001", 10000);

        // 2. Send refund request
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setRefundAmountInPaise(5000);  // Partial refund
        request.setReason("Customer request");

        mockMvc.perform(
            post("/api/v1/payments/" + payment.getPaymentUuid() + "/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.refundId").exists())
        .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    public void testRefundNonSuccessfulPaymentFails() throws Exception {
        // 1. Create a pending payment
        Payment payment = createPaymentWithStatus("MER001", 10000, PaymentStatus.PENDING);

        // 2. Send refund request (should fail)
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setRefundAmountInPaise(5000);

        mockMvc.perform(
            post("/api/v1/payments/" + payment.getPaymentUuid() + "/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
    }
}
```

---

## State Machine Validation

### Valid Transitions

**PaymentStatus** transitions:

```
CREATED
  └─→ QR_GENERATED (backend generates QR)
  └─→ CANCELLED (merchant cancels)

QR_GENERATED
  └─→ PENDING (customer scans & approves)
  └─→ EXPIRED (15 minutes pass, checked on-read)
  └─→ CANCELLED (merchant cancels)

PENDING
  └─→ SUCCESS (PSP confirms)
  └─→ FAILED (PSP declines)
  └─→ EXPIRED (15 minutes pass, checked on-read)

SUCCESS / FAILED / EXPIRED / CANCELLED
  └─→ (TERMINAL - no further changes)
```

### Validation Code

```java
private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = Map.ofEntries(
    Map.entry(PaymentStatus.CREATED, Set.of(PaymentStatus.QR_GENERATED, PaymentStatus.CANCELLED)),
    Map.entry(PaymentStatus.QR_GENERATED, Set.of(PaymentStatus.PENDING, PaymentStatus.EXPIRED, PaymentStatus.CANCELLED)),
    Map.entry(PaymentStatus.PENDING, Set.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.EXPIRED)),
    Map.entry(PaymentStatus.SUCCESS, Set.of()),
    Map.entry(PaymentStatus.FAILED, Set.of()),
    Map.entry(PaymentStatus.EXPIRED, Set.of()),
    Map.entry(PaymentStatus.CANCELLED, Set.of())
);

public void validateTransition(PaymentStatus from, PaymentStatus to) {
    Set<PaymentStatus> allowedTransitions = VALID_TRANSITIONS.get(from);
    if (!allowedTransitions.contains(to)) {
        throw new InvalidStateException(
            String.format(
                "Cannot transition from %s to %s. Allowed: %s",
                from, to, allowedTransitions
            )
        );
    }
}
```

---

## Testing Patterns

### Pattern 1: Clock Injection for Time-Dependent Tests

**Problem**: How to test 15-minute expiry without waiting?

**Solution**: Inject mockable Clock bean

```java
// In config
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();  // Production
    }
}

// In test
@SpringBootTest
public class PaymentExpiryTest {

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private Clock clock;  // Override with test clock

    @Test
    public void testPaymentExpiresAfter15Minutes() {
        // 1. Create payment at time T
        Instant createdTime = Instant.parse("2026-01-10T10:00:00Z");
        Mockito.when(clock.instant()).thenReturn(createdTime);

        Payment payment = paymentService.createPayment("MER001", BigDecimal.valueOf(250));
        assert payment.getExpiresAt().equals(createdTime.plus(15, ChronoUnit.MINUTES));

        // 2. Move clock forward 16 minutes
        Instant futureTime = createdTime.plus(16, ChronoUnit.MINUTES);
        Mockito.when(clock.instant()).thenReturn(futureTime);

        // 3. Payment should be marked EXPIRED on read
        Payment fetched = paymentService.getPayment(payment.getPaymentUuid());
        assert fetched.getStatus().equals(PaymentStatus.EXPIRED);
    }
}
```

### Pattern 2: TransactionalTest for Rollback

```java
@SpringBootTest
@Transactional
public class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    public void testPaymentCreationRollsBackOnException() {
        // This test will automatically rollback after execution
        // So database remains clean

        try {
            paymentService.createPaymentWithInvalidMerchant("INVALID");
            fail("Should have thrown exception");
        } catch (ResourceNotFoundException e) {
            // Expected
        }

        // Verify payment wasn't saved (automatic rollback)
        assert paymentRepository.count() == 0;
    }
}
```

### Pattern 3: Repository Query Testing

```java
@DataJpaTest
public class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testFindExpiredPayments() {
        // 1. Create test data
        Merchant merchant = entityManager.persistAndFlush(
            new Merchant(/* ... */)
        );

        // Create expired payment
        Payment expired = new Payment(/* status: PENDING, expiresAt: now - 1 min */);
        entityManager.persistAndFlush(expired);

        // Create non-expired payment
        Payment pending = new Payment(/* status: PENDING, expiresAt: now + 15 min */);
        entityManager.persistAndFlush(pending);

        // 2. Query
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments();

        // 3. Assert
        assert expiredPayments.size() == 1;
        assert expiredPayments.get(0).equals(expired);
    }
}
```

---

## Performance Tuning

### Index Strategy

**Hot Query 1**: List merchant's payments (filtered by status)

```sql
-- Composite index covers all predicates in order
CREATE INDEX idx_payment_main ON payment_order(merchant_id, status, created_at);

-- Query execution plan (should be INDEX RANGE SCAN)
SELECT * FROM payment_order
WHERE merchant_id = 1 AND status = 'SUCCESS'
ORDER BY created_at DESC
LIMIT 20;
```

**Hot Query 2**: Find expired payments

```sql
CREATE INDEX idx_payment_expires ON payment_order(expires_at, status);

-- Query execution plan (should be INDEX RANGE SCAN)
SELECT * FROM payment_order
WHERE expires_at < CURRENT_TIMESTAMP
AND status IN ('PENDING', 'QR_GENERATED');
```

### Query Optimization

**❌ Bad**: N+1 queries

```java
List<Payment> payments = paymentRepository.findAll();  // Query 1
for (Payment p : payments) {
    Merchant m = p.getMerchant();  // Query 2, 3, 4... (N more queries!)
    System.out.println(m.getName());
}
```

**✅ Good**: Single query with JOIN

```java
@Query("""
    SELECT p FROM Payment p
    JOIN FETCH p.merchant
    WHERE p.status = 'SUCCESS'
""")
List<Payment> findSuccessfulPaymentsWithMerchant();
```

### Connection Pool Tuning

Edit `application.properties`:

```properties
# HikariCP (default connection pool)
spring.datasource.hikari.maximum-pool-size=20  # Max connections
spring.datasource.hikari.minimum-idle=5        # Min idle
spring.datasource.hikari.connection-timeout=30000  # 30 sec timeout
```

---

## Common Mistakes

### Mistake 1: Using `System.currentTimeMillis()` instead of Clock

❌ **Bad** (not testable):
```java
payment.setCreatedAt(Instant.ofEpochMilli(System.currentTimeMillis()));
```

✅ **Good** (testable with mocked Clock):
```java
@Autowired
private Clock clock;

payment.setCreatedAt(Instant.now(clock));
```

---

### Mistake 2: Exposing Entities in API Responses

❌ **Bad** (exposes all fields, hard to version):
```java
@GetMapping("/{id}")
public Payment getPayment(@PathVariable String id) {
    return paymentRepository.findByPaymentUuid(id).orElseThrow();
}
```

✅ **Good** (controlled API contract via DTO):
```java
@GetMapping("/{id}")
public ResponseEntity<PaymentResponse> getPayment(@PathVariable String id) {
    Payment payment = paymentRepository.findByPaymentUuid(id)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    return ResponseEntity.ok(new PaymentResponse(payment));
}
```

---

### Mistake 3: Performing State Transition Without Validation

❌ **Bad** (allows invalid states):
```java
payment.setStatus(PaymentStatus.SUCCESS);  // What if payment was already CANCELLED?
paymentRepository.save(payment);
```

✅ **Good** (validates precondition):
```java
if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
    throw new InvalidStateException("Payment must be PENDING");
}
payment.setStatus(PaymentStatus.SUCCESS);
paymentRepository.save(payment);
```

---

### Mistake 4: Using Floating-Point for Money

❌ **Bad** (rounding errors):
```java
double amount = 250.00;
double fee = amount * 0.025;  // 6.25, but might be 6.249999999999999
```

✅ **Good** (exact amounts in paise):
```java
long amountInPaise = 25000;  // ₹250.00
long fee = amountInPaise * 25 / 1000;  // 625 paise (exact)
```

---

### Mistake 5: Forgetting @Transactional Annotation

❌ **Bad** (no transaction, partial failure risk):
```java
public void createPaymentAndEvent(String merchantId, BigDecimal amount) {
    Payment payment = paymentRepository.save(createPayment(merchantId, amount));
    paymentEventRepository.save(createEvent(payment));  // If this fails, payment is saved but event is not!
}
```

✅ **Good** (all-or-nothing):
```java
@Transactional
public void createPaymentAndEvent(String merchantId, BigDecimal amount) {
    Payment payment = paymentRepository.save(createPayment(merchantId, amount));
    paymentEventRepository.save(createEvent(payment));  // If this fails, entire transaction rolls back
}
```

---

## Troubleshooting

### Issue 1: "Merchant not found" when creating payment

**Symptoms**:
```
ERROR: ResourceNotFoundException: Merchant not found: MERCHANT_001
```

**Diagnosis**:
1. Check test data in `DataInitializer.java`
2. Verify merchant ID matches exactly (case-sensitive)
3. Check if merchant is ACTIVE status

**Fix**:
```bash
# 1. Check H2 console
curl http://localhost:8080/h2-console
# Query: SELECT * FROM merchant;

# 2. Verify merchant ID in test data
# Edit: src/main/java/org/personal/config/DataInitializer.java
# Ensure merchant.setMerchantUuid("MER001");

# 3. Restart backend
mvn spring-boot:run
```

---

### Issue 2: "Payment is in CREATED state, expected PENDING"

**Symptoms**:
```
ERROR: InvalidStateException: Payment is in CREATED state, expected PENDING
```

**Diagnosis**:
1. Payment hasn't moved to QR_GENERATED yet
2. QR generation endpoint not called
3. Or payment expired

**Fix**:
```bash
# 1. Create payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"merchantId": "MER001", "amount": 250}'

# Response:
# {"paymentId": "...", "status": "CREATED"}

# 2. Generate QR (moves to QR_GENERATED)
curl http://localhost:8080/api/v1/payments/{paymentId}/qr

# 3. Now payment is QR_GENERATED, ready for polling
curl http://localhost:8080/api/v1/payments/{paymentId}
```

---

### Issue 3: "Column 'PAYMENT_UUID' not found"

**Symptoms**:
```
ERROR: Column "PAYMENT_UUID" not found
```

**Diagnosis**:
1. H2 database file created before UUID columns added
2. JPA `ddl-auto=update` didn't add columns to existing schema

**Fix**:
```bash
# 1. Delete H2 database files
rm paymentdb.mv.db paymentdb.trace.db

# 2. Restart backend (H2 will auto-create schema)
mvn spring-boot:run

# Verify: Check http://localhost:8080/h2-console
# Query: DESCRIBE payment_order;
# Should show payment_uuid column
```

---

### Issue 4: Tests Failing with "No qualifying bean of type Clock"

**Symptoms**:
```
ERROR: No qualifying bean of type 'java.time.Clock' available
```

**Diagnosis**:
1. Clock bean not defined in test context
2. Spring can't inject Clock in test

**Fix**:
```java
@SpringBootTest
public class PaymentServiceTest {

    @MockBean
    private Clock clock;  // Add this to provide Clock for injection

    @Autowired
    private PaymentService paymentService;

    // Tests now work...
}
```

---

### Issue 5: Performance: Queries Taking > 1 Second

**Symptoms**:
```
WARN: Query took 1234ms (expected < 10ms)
```

**Diagnosis**:
1. Missing index on filter column
2. Fetching related entities without JOIN FETCH
3. No pagination (loading all rows)

**Fix**:

```java
// ❌ Bad: Loads all payments (no pagination, no index on status)
List<Payment> all = paymentRepository.findAll();

// ✅ Good: Uses index, paginated
Page<Payment> page = paymentRepository.findByMerchantAndStatus(
    merchantId,
    PaymentStatus.SUCCESS,
    PageRequest.of(0, 20)  // First 20 results
);

// ✅ Good: JOIN FETCH to avoid N+1
@Query("""
    SELECT p FROM Payment p
    JOIN FETCH p.merchant
    WHERE p.status = 'SUCCESS'
""")
List<Payment> findSuccessfulPaymentsWithMerchant();
```

---

### Issue 6: Payment not transitioning to SUCCESS

**Symptoms**:
```
Payment created, polling returns PENDING forever
```

**Diagnosis**:
1. PSP adapter not implemented (Phase-2)
2. PaymentTransaction not created
3. Status update not called

**Fix** (Phase-1 workaround):
```bash
# Manually test state transition via direct database update
# 1. Open H2 console: http://localhost:8080/h2-console
# 2. Run:
UPDATE payment_order SET status = 'PENDING' WHERE payment_uuid = '...';
UPDATE payment_transaction SET status = 'SUCCESS' WHERE payment_order_id = (...);

# 3. Poll again:
curl http://localhost:8080/api/v1/payments/{paymentId}
# Should return status: SUCCESS

# Note: Phase-2 will auto-update via PSP webhook
```

---

## FAQ

**Q: When does @Transactional rollback?**
A: When an unchecked exception (RuntimeException subclass) is thrown. Checked exceptions don't rollback by default.

**Q: Can I override the rollback behavior?**
A: Yes, use `@Transactional(rollbackFor = Exception.class)` to rollback on checked exceptions.

**Q: Should I test Repository methods?**
A: For complex queries with @Query, yes. For simple finders, Spring Data tests them.

**Q: How do I add a new field to Payment entity?**
A: Add `@Column` field, getter, setter. Restart app. JPA `ddl-auto=update` will add the column.

**Q: What happens if I delete a Merchant?**
A: FK constraint with RESTRICT prevents deletion if payments exist. Good for data integrity.

**Q: How do I seed test data on startup?**
A: Create a `DataInitializer` class with `@Component` and `@PostConstruct`. It runs on app startup.

---

## Related Documentation

- **[./DOMAIN_MODEL.md](./DOMAIN_MODEL.md)** - Entity definitions & state machines
- **[./DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)** - DDL, indexes, query patterns
- **[./IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)** - Step-by-step development roadmap
- **[../README.md](../README.md)** - Quick start & test credentials

---

**Last Updated**: 2026-01-10
**Version**: Phase-1.0
