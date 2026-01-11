# 🛠️ Implementation Plan (Phase-1)

## Timeline: 2-3 Days

**Sprint Goal**: Build production-grade QR payment gateway with both UPI (legacy) and QR (new) systems coexisting.

---

## Phase A: Project Setup & Configuration (1 Day)

### A1: Create New Package Structure

**Location**: `src/main/java/org/personal/`

```
org/personal/
├── entity/
│   ├── (existing: Merchant, Payment, Transaction)
│   ├── (new: MasterMerchant, PaymentOrder, QrCode)
│   ├── PaymentEvent.java
│   └── RefundOrder.java
│
├── dto/
│   ├── (existing: responses)
│   ├── (new: PaymentOrderRequest.java)
│   ├── (new: PaymentOrderResponse.java)
│   ├── (new: QrCodeResponse.java)
│   └── (new: RefundOrderRequest/Response.java)
│
├── controller/
│   ├── (existing: PaymentController, TransactionController)
│   ├── (new: PaymentOrderController.java)
│   ├── (new: MerchantController.java)
│   └── (new: RefundController.java)
│
├── service/
│   ├── (existing: PaymentService)
│   ├── (new: PaymentOrderService.java)
│   ├── (new: QrCodeService.java)
│   ├── (new: MerchantService.java)
│   └── (new: RefundService.java)
│
├── repository/
│   ├── (existing: MerchantRepository, PaymentRepository)
│   ├── (new: MasterMerchantRepository.java)
│   ├── (new: PaymentOrderRepository.java)
│   ├── (new: QrCodeRepository.java)
│   ├── (new: PaymentEventRepository.java)
│   └── (new: RefundOrderRepository.java)
│
├── psp/
│   ├── (new: UpiPspAdapter.java) [Interface]
│   ├── (new: MockBankUpiAdapter.java) [Implementation]
│   └── (new: UpiPspResponse.java) [DTO]
│
├── enums/
│   ├── (new: MerchantStatus.java)
│   ├── (new: PaymentStatus.java)
│   ├── (new: TransactionStatus.java)
│   ├── (new: PaymentEventType.java)
│   ├── (new: RefundStatus.java)
│   └── (new: QrType.java)
│
├── exception/
│   ├── (existing: ResourceNotFoundException)
│   ├── (new: InvalidPaymentTransitionException.java)
│   ├── (new: PaymentExpiredException.java)
│   └── (new: MerchantInactiveException.java)
│
├── config/
│   ├── (existing: CorsConfig.java)
│   ├── (new: ClockProvider.java) [For time-travel testing]
│   └── (new: DataInitializerQr.java) [Seed new data]
│
└── util/
    ├── (new: QrCodeGenerator.java)
    └── (new: PaymentCalculator.java)
```

### A2: Update pom.xml

**Add Dependencies**:
```xml
<!-- QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>

<!-- JSON Processing (for metadata) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- Lombok (optional, for boilerplate reduction) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
</dependency>
```

### A3: Update application.properties

```properties
# Existing H2 config (unchanged)
spring.datasource.url=jdbc:h2:file:./paymentdb
spring.h2.console.enabled=true

# API Versioning
api.version=v1

# QR Configuration
qr.expiry.minutes=15
qr.size.pixels=400

# PSP Configuration (Mock)
psp.mock.enabled=true
psp.mock.success.probability=0.8
psp.mock.approval.delay.seconds=5
```

### A4: Create Enums

Create all enum classes in `org.personal.enums/`:
- MerchantStatus
- PaymentStatus
- TransactionStatus
- PaymentEventType
- RefundStatus
- QrType

---

## Phase B: Entity Layer (1 Day)

### B1: Create Entity Classes

**Files to create** in `org.personal.entity/`:
1. MasterMerchant.java
2. PaymentOrder.java
3. PaymentTransaction.java (update existing structure)
4. QrCode.java
5. PaymentEvent.java
6. RefundOrder.java

**Implementation Notes**:
- Use `@Entity @Table` annotations
- Add `@Index` annotations for performance
- Implement `@PrePersist`, `@PreUpdate` for timestamps
- Use `@Enumerated(EnumType.STRING)` for enums
- Add `@ColumnDefinition("JSON")` for metadata

### B2: Create Repository Interfaces

**Files to create** in `org.personal.repository/`:
1. MasterMerchantRepository.java
   - `findByMasterMerchantUuid(String uuid)`
   - `existsByMasterMerchantUuid(String uuid)`

2. PaymentOrderRepository.java
   - `findByPaymentUuid(String uuid)`
   - `findByMerchantIdAndStatus(Long merchantId, PaymentStatus status, Pageable p)`
   - `findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime expiresAt)`

3. PaymentEventRepository.java
   - `findByPaymentUuidOrderByCreatedAtAsc(String paymentUuid)`

4. QrCodeRepository.java
   - `findByPaymentOrderId(Long paymentOrderId)`

5. RefundOrderRepository.java
   - `findByPaymentOrderId(Long paymentOrderId)`

### B3: Schema Generation

Run Spring Boot:
```bash
mvn spring-boot:run
```

H2 will auto-create all tables via JPA `ddl-auto=update`.

Verify in H2 Console:
```
http://localhost:8080/h2-console
```

---

## Phase C: Service Layer (1 Day)

### C1: Utility Services

**1. QrCodeGenerator.java** (in `org.personal.util/`)
```java
public class QrCodeGenerator {
    public QrGenerationResult generateDynamicQr(
        String merchantVpa,
        long amountInPaise,
        String merchantName,
        String orderId
    ) {
        // Generate UPI intent string
        // Generate QR code (Base64 PNG + SVG)
        // Return QrGenerationResult
    }
}
```

**2. PaymentCalculator.java** (in `org.personal.util/`)
```java
public class PaymentCalculator {
    public FeeCalculation calculateFees(
        long amountInPaise,
        BigDecimal platformFeePercent
    ) {
        // Calculate platform fee
        // Calculate merchant net
        // Return FeeCalculation
    }
}
```

**3. ClockProvider.java** (in `org.personal.config/`)
```java
@Configuration
public class ClockProvider {
    @Bean
    public Clock clock() {
        return Clock.systemUTC(); // Mockable for testing
    }
}
```

### C2: PSP Adapter

**1. UpiPspAdapter.java** (Interface in `org.personal.psp/`)
```java
public interface UpiPspAdapter {
    QrGenerationResult generateDynamicQr(QrRequest request);
    PaymentStatusResponse fetchPaymentStatus(String pspTransactionId);
}
```

**2. MockBankUpiAdapter.java** (Implementation)
```java
@Service
public class MockBankUpiAdapter implements UpiPspAdapter {
    // Simulate QR generation
    // Simulate payment status changes
    // Return realistic responses
}
```

### C3: Service Classes

**1. MasterMerchantService.java**
```java
@Service
public class MasterMerchantService {
    - createMasterMerchant(CreateMasterMerchantRequest)
    - getMasterMerchant(uuid)
    - listMasterMerchants()
}
```

**2. MerchantService.java** (update existing)
```java
// Add new methods:
- createMerchantUnder(masterMerchantId, CreateMerchantRequest)
- getMerchantByUuid(uuid)
- listChildMerchants(masterMerchantId)
- validateMerchantActive(merchantId) [throws MerchantInactiveException]
```

**3. PaymentOrderService.java** (NEW - Core logic)
```java
@Service
@Transactional
public class PaymentOrderService {

    // CREATE PAYMENT + QR
    public PaymentOrderResponse createPayment(CreatePaymentOrderRequest) {
        // 1. Validate merchant exists & active
        // 2. Validate amount > 0
        // 3. Create PaymentOrder (status=CREATED)
        // 4. Calculate fees
        // 5. Generate QR via QrCodeService
        // 6. Update status to QR_GENERATED
        // 7. Create PaymentEvent(CREATED, QR_GENERATED)
        // 8. Return response
    }

    // GET PAYMENT (with expiry check)
    public PaymentOrderResponse getPayment(paymentUuid) {
        // 1. Find PaymentOrder
        // 2. IF (now() > expiresAt AND status=PENDING) {
        //      UPDATE status = EXPIRED
        //      CREATE PaymentEvent(EXPIRED)
        //    }
        // 3. Fetch latest PaymentTransaction (if exists)
        // 4. Return response with transaction
    }

    // STATE MACHINE VALIDATION
    private void validateStateTransition(PaymentStatus from, PaymentStatus to) {
        // Enforce allowed transitions
        // Throw InvalidPaymentTransitionException otherwise
    }

    // HELPER: Evaluate Expiry
    private boolean shouldExpire(PaymentOrder payment) {
        return Instant.now().isAfter(payment.getExpiresAt())
            && payment.getStatus() == PaymentStatus.PENDING;
    }
}
```

**4. QrCodeService.java**
```java
@Service
public class QrCodeService {

    public QrCodeResponse generateQr(PaymentOrder payment) {
        // 1. Build UPI intent string
        // 2. Generate QR via QrCodeGenerator
        // 3. Create QrCode entity
        // 4. Save to repository
        // 5. Return QrCodeResponse
    }

    public QrCodeResponse getQr(paymentUuid) {
        // Return QR details (Base64, SVG, intent)
    }
}
```

**5. PaymentEventService.java**
```java
@Service
public class PaymentEventService {

    public void recordEvent(
        PaymentOrder payment,
        PaymentEventType eventType,
        Map<String, Object> metadata
    ) {
        // Create PaymentEvent entity
        // Save to repository
    }

    public List<PaymentEventResponse> getAuditTrail(paymentUuid) {
        // Return all events for payment (ordered by time)
    }
}
```

**6. RefundService.java**
```java
@Service
public class RefundService {

    public RefundOrderResponse createRefund(
        paymentUuid,
        CreateRefundRequest
    ) {
        // 1. Validate payment exists & SUCCESS
        // 2. Validate refund amount <= payment amount
        // 3. Create RefundOrder (status=INITIATED)
        // 4. Call PSP to process refund
        // 5. Update status (SUCCESS/FAILED)
        // 6. Record PaymentEvent(REFUND_INITIATED)
        // 7. Return response
    }

    public RefundOrderResponse getRefund(refundUuid) {
        // Return refund details
    }
}
```

---

## Phase D: Controller Layer (Half Day)

### D1: API Controllers

**1. MerchantController.java**
```java
@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {
    - POST /merchants
    - GET /merchants/{merchantId}
    - GET /master-merchants/{masterId}/merchants
}
```

**2. PaymentOrderController.java**
```java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentOrderController {
    - POST /payments (create + QR)
    - GET /payments/{paymentId}
    - GET /merchants/{merchantId}/payments (list)
    - GET /payments/{paymentId}/qr
    - POST /payments/{paymentId}/cancel
}
```

**3. RefundController.java**
```java
@RestController
@RequestMapping("/api/v1")
public class RefundController {
    - POST /payments/{paymentId}/refunds
    - GET /refunds/{refundId}
}
```

**4. AuditController.java**
```java
@RestController
@RequestMapping("/api/v1")
public class AuditController {
    - GET /payments/{paymentId}/events (audit trail)
}
```

### D2: DTOs

Create request/response DTOs:
- CreatePaymentOrderRequest
- PaymentOrderResponse
- QrCodeResponse
- CreateRefundRequest
- RefundOrderResponse
- PaymentEventResponse
- MerchantCreateRequest
- MerchantResponse

---

## Phase E: Exception Handling & Validation (Half Day)

### E1: Custom Exceptions

```java
// In org.personal.exception/

public class InvalidPaymentTransitionException extends RuntimeException {}
public class PaymentExpiredException extends RuntimeException {}
public class MerchantInactiveException extends RuntimeException {}
public class InvalidRefundAmountException extends RuntimeException {}
```

### E2: Global Exception Handler

**Update GlobalExceptionHandler.java**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPaymentTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(...) {
        // Return 400 with error details
    }

    @ExceptionHandler(PaymentExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(...) {
        // Return 410 Gone
    }

    @ExceptionHandler(MerchantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleInactiveM merchant(...) {
        // Return 409 Conflict
    }
}
```

---

## Phase F: Data Initialization (Half Day)

### F1: DataInitializerQr.java

```java
@Component
public class DataInitializerQr implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Create MasterMerchant
        // Create 3 ChildMerchants with VPAs
        // Log initialization details
    }
}
```

### F2: Test Data Script (API Usage)

Create `doc/curl-examples.sh`:
```bash
#!/bin/bash

# Create Master Merchant
curl -X POST http://localhost:8080/api/v1/master-merchants \
  -H "Content-Type: application/json" \
  -d '{"name":"Payment Gateway Ltd"}'

# Create Child Merchant
curl -X POST http://localhost:8080/api/v1/merchants \
  -H "Content-Type: application/json" \
  -d '{"masterMerchantId":"...","name":"Store A","vpa":"storea@axis"}'

# Create Payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"...","amountInPaise":10050,"currency":"INR"}'
```

---

## Phase G: Testing Strategy

### G1: Unit Tests

```
src/test/java/org/personal/
├── service/
│   ├── PaymentOrderServiceTest.java
│   ├── QrCodeServiceTest.java
│   ├── RefundServiceTest.java
│   └── MerchantServiceTest.java
└── util/
    └── PaymentCalculatorTest.java
```

### G2: Integration Tests

```
src/test/java/org/personal/
└── integration/
    ├── PaymentCreationFlowTest.java
    ├── PaymentExpiryTest.java
    ├── RefundFlowTest.java
    └── ApiEndpointTest.java
```

### G3: Test Utilities

```java
// TestClockProvider.java
@TestConfiguration
public class TestClockProvider {
    @Bean
    public Clock fixedClock() {
        return Clock.fixed(...);
    }
}

// TestDataFactory.java
public class TestDataFactory {
    static MasterMerchant createTestMasterMerchant() {}
    static Merchant createTestMerchant() {}
    static PaymentOrder createTestPaymentOrder() {}
}
```

---

## Phase H: Documentation

### H1: README.md (Product-focused)

- What it does
- Quick start (5 minutes)
- Test credentials
- API overview
- Link to developer docs

### H2: DEVELOPER_GUIDE.md (Technical)

- Architecture diagram
- Code structure
- How to add a new endpoint
- How to extend PSP adapter
- Database schema
- Troubleshooting

### H3: API_EXAMPLES.md

- cURL examples for all endpoints
- Postman collection (JSON)
- Example payloads & responses

---

## Phase I: Verification Checklist

### I1: Code Quality

- [ ] All entities have proper JPA annotations
- [ ] All repositories extend JpaRepository
- [ ] All services have @Transactional where needed
- [ ] All exceptions are custom & meaningful
- [ ] State machine validated (no invalid transitions)
- [ ] Soft deletes work correctly
- [ ] Indexes are applied correctly

### I2: API Contract

- [ ] All endpoints respond with correct format
- [ ] Error handling returns proper HTTP codes
- [ ] UUID fields are unique and indexed
- [ ] Paise amounts are BIGINT (no decimals)
- [ ] Timestamps are ISO 8601 format
- [ ] All responses include success/error structure

### I3: Business Logic

- [ ] Expiry evaluation on-read works
- [ ] Fee calculation is accurate
- [ ] State transitions are enforced
- [ ] Soft deletes don't leak in queries
- [ ] Idempotency keys work
- [ ] Refunds validate correctly

### I4: Database

- [ ] All tables created
- [ ] All indexes applied
- [ ] Foreign key constraints work
- [ ] Soft deletes tested
- [ ] Data integrity maintained

### I5: Testing

- [ ] Unit tests pass (>80% coverage)
- [ ] Integration tests pass
- [ ] API tests verify contracts
- [ ] Time-travel tests for expiry logic

### I6: Documentation

- [ ] README is clear & complete
- [ ] Developer guide covers architecture
- [ ] API examples are working
- [ ] Troubleshooting includes common issues

---

## Deliverables by Phase

| Phase | Deliverable | Status |
|---|---|---|
| A | Project structure, pom.xml, config | TBD |
| B | All entities, repositories, schema | TBD |
| C | Services, adapters, utilities | TBD |
| D | Controllers, DTOs, API endpoints | TBD |
| E | Exception handling, validation | TBD |
| F | Data initialization, test scripts | TBD |
| G | Unit & integration tests | TBD |
| H | README, developer guide, examples | TBD |
| I | Verification & sign-off | TBD |

---

## Code Review Checklist

Before merging:

- [ ] No hardcoded values (use config)
- [ ] No N+1 queries
- [ ] No open transactions
- [ ] No missing null checks
- [ ] No TODOs or FIXMEs
- [ ] All javadoc complete
- [ ] Test coverage > 80%
- [ ] No debug logs left in code
- [ ] Proper logging levels (INFO, WARN, ERROR)

---

## Performance Expectations

| Operation | Expected Time | Notes |
|---|---|---|
| Create payment | < 100ms | Includes QR generation |
| Get payment | < 10ms | Index lookup |
| List payments | < 100ms | With pagination |
| Refund creation | < 200ms | Includes PSP call |
| Audit trail fetch | < 50ms | Index on event time |

---

This plan is **executable** and **non-blocking**. Each phase can proceed independently after prerequisites are met.
