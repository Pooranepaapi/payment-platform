# 🏗️ Code Scaffolding Templates

Quick-start templates for implementing new features in the payment gateway.

---

## Overview

This folder contains **ready-to-use code templates** for:
- **Entities** (JPA models)
- **Repositories** (Data access)
- **Services** (Business logic)
- **Controllers** (HTTP endpoints)
- **DTOs** (Request/response objects)
- **Exceptions** (Error handling)
- **Tests** (Unit & integration tests)

Each template includes:
✅ Complete JavaDoc with examples
✅ Best practices & design patterns
✅ Comments explaining design decisions
✅ Copy-paste ready code

---

## Quick Start: Add a New Feature

### Example: Implement Refund Feature

#### Step 1: Copy Templates

```bash
cd doc/scaffolds

# Copy entity template
cp Entity.template.java ../../src/main/java/org/personal/entity/RefundOrder.java

# Copy repository template
cp Repository.template.java ../../src/main/java/org/personal/repository/RefundOrderRepository.java

# Copy service template
cp Service.template.java ../../src/main/java/org/personal/service/RefundService.java

# Copy controller template
cp Controller.template.java ../../src/main/java/org/personal/controller/RefundController.java

# Copy DTOs
cp DTOs.template.java ../../src/main/java/org/personal/dto/RefundDTOs.java

# Copy test template
cp Test.template.java ../../src/test/java/org/personal/service/RefundServiceTest.java
```

#### Step 2: Replace Placeholders

**In each file, find and replace:**

| Placeholder | Example | Description |
|---|---|---|
| `[ENTITY_NAME]` | `RefundOrder` | Class name |
| `[table_name]` | `refundOrder` | camelCase version |
| `[TABLE_NAME]` | `refund_order` | snake_case table name |
| `[API_PATH]` | `refunds` | URL path |
| `[ENUM_NAME]` | `RefundStatus` | Enum class name |
| `[ENUM_STATUS]` | `RefundStatus` | Enum value |

**Quick Replace (VS Code):**
```
Ctrl+H (Find and Replace)
Find: [ENTITY_NAME]
Replace: RefundOrder
Replace All
```

#### Step 3: Implement Methods

Each template has `// TODO` comments marking sections to implement:

1. **Entity.template.java**
   - Add `@Column` fields for your business logic
   - Add enum fields with `@Enumerated(EnumType.STRING)`
   - Generate getters/setters (IDE: Ctrl+Shift+P)

2. **Repository.template.java**
   - Implement custom `@Query` methods for your use cases
   - Keep simple queries as method names

3. **Service.template.java**
   - Implement create/get/update/delete operations
   - Add business logic and validation
   - Use `@Transactional` for data consistency

4. **Controller.template.java**
   - Add request validation
   - Map HTTP methods to service calls
   - Return proper HTTP status codes

5. **DTOs.template.java**
   - Define request fields (Create[ENTITY_NAME]Request)
   - Define response fields ([ENTITY_NAME]Response)
   - Use `@JsonProperty` for field mapping

6. **Test.template.java**
   - Replace mock setup in `setUp()`
   - Implement test cases for business logic
   - Test both success and failure scenarios

#### Step 4: Compile and Test

```bash
# Compile
mvn clean compile

# Run tests
mvn test -Dtest=RefundServiceTest

# Run all tests
mvn test

# Run application
mvn spring-boot:run
```

---

## Template Details

### Entity.template.java

**When to use**: Creating a new JPA entity (database table)

**Key sections**:
- Primary key (`@Id`)
- UUID field (external reference)
- Foreign keys (`@ManyToOne`)
- Enum fields (`@Enumerated`)
- Timestamps (Instant, not LocalDateTime)
- Soft delete (`deletedAt`)

**After copying**:
1. Add `@Column` fields for your domain
2. Add relationships (`@ManyToOne`, `@OneToMany`)
3. Generate getters/setters

**Example**:
```java
public class RefundOrder {
    private Long id;
    private String refundOrderUuid;
    private String reason;  // Add this field
    private RefundStatus status;
    private Long refundAmountInPaise;
    private Instant createdAt;
    private Instant updatedAt;
}
```

---

### Repository.template.java

**When to use**: Creating a Spring Data JPA repository

**Key sections**:
- Simple queries (method names)
- Complex queries (`@Query`)
- Batch operations
- Native SQL (only if necessary)

**Query method naming**:
- `findBy[Field]`: Single result
- `findAllBy[Field]`: Multiple results
- `existsBy[Field]`: Boolean
- `countBy[Field]`: Count
- `deleteBy[Field]`: Delete

**After copying**:
1. Replace `[ENTITY_NAME]` with your entity
2. Add query methods for your use cases
3. Use `@Query` for complex WHERE clauses

**Example**:
```java
public interface RefundOrderRepository extends JpaRepository<RefundOrder, Long> {
    Optional<RefundOrder> findByRefundOrderUuid(String uuid);

    Page<RefundOrder> findByPaymentIdAndStatus(Long paymentId, RefundStatus status, Pageable pageable);

    List<RefundOrder> findByStatusAndCreatedAtGreaterThan(RefundStatus status, Instant date);
}
```

---

### Service.template.java

**When to use**: Creating a service for business logic

**Key sections**:
- Create operations (with validation)
- Read operations (with optional filtering)
- Update operations (state transitions)
- Delete operations (soft delete)
- Business logic (fee calculations, validations)
- Batch operations

**Patterns**:
- Use `@Autowired` for dependencies
- Use `@Transactional` for atomicity
- Use `Clock` for testable timestamps
- Validate preconditions before operations
- Record events for audit trail

**After copying**:
1. Inject repositories and services
2. Implement create/get/update/delete
3. Add state machine validation
4. Record events

**Example**:
```java
@Service
@Transactional
public class RefundService {

    public RefundOrder createRefund(String paymentId, Long amount, String reason) {
        // 1. Validate preconditions
        Payment payment = paymentRepository.findByPaymentUuid(paymentId).orElseThrow();
        if (!payment.getStatus().equals(PaymentStatus.SUCCESS)) {
            throw new InvalidStateException("Only successful payments can be refunded");
        }

        // 2. Create entity
        RefundOrder refund = new RefundOrder();
        refund.setRefundOrderUuid(UUID.randomUUID().toString());
        // ... set other fields

        // 3. Persist
        RefundOrder saved = refundOrderRepository.save(refund);

        // 4. Record event
        paymentEventService.recordEvent(payment, PaymentEventType.REFUND_INITIATED, ...);

        return saved;
    }
}
```

---

### Controller.template.java

**When to use**: Creating REST API endpoints

**HTTP Methods**:
- `@PostMapping`: Create (201)
- `@GetMapping`: Retrieve (200)
- `@PutMapping`: Update (200)
- `@DeleteMapping`: Delete (204)
- `@PatchMapping`: Partial update (200)

**Key sections**:
- Input validation
- Service calls
- DTO conversion
- HTTP status codes
- Error handling via @ExceptionHandler

**After copying**:
1. Replace `[API_PATH]` with your endpoint path
2. Add request/response methods
3. Validate input
4. Call service

**Example**:
```java
@RestController
@RequestMapping("/api/v1/refunds")
public class RefundController {

    @PostMapping
    public ResponseEntity<RefundResponse> create(
        @RequestBody CreateRefundRequest request) {

        RefundOrder refund = refundService.createRefund(
            request.getPaymentId(),
            request.getAmount(),
            request.getReason()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RefundResponse(refund));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundResponse> get(@PathVariable String id) {
        RefundOrder refund = refundService.getByUuid(id);
        return ResponseEntity.ok(new RefundResponse(refund));
    }
}
```

---

### DTOs.template.java

**When to use**: Creating request/response objects for API

**Key sections**:
- Create[Entity]Request: POST body
- Update[Entity]Request: PUT body
- [Entity]Response: GET response
- [Entity]DetailResponse: Extended response with relationships
- PageResponse: Pagination wrapper
- ErrorResponse: Error responses

**Rules**:
- Never expose JPA entities directly
- Use `@JsonProperty` for field mapping
- Use BigDecimal for amounts
- Use Instant for timestamps
- Constructor from entity for conversion

**After copying**:
1. Add fields for your domain
2. Create constructor from entity
3. Use `@JsonProperty` for API field names

**Example**:
```java
@Getter @Setter
public class CreateRefundRequest {
    private String paymentId;
    private Long refundAmountInPaise;
    private String reason;
}

@Getter @Setter
public class RefundResponse {
    @JsonProperty("refund_id")
    private String uuid;
    private String status;
    private BigDecimal amount;

    public RefundResponse(RefundOrder refund) {
        this.uuid = refund.getRefundOrderUuid();
        this.status = refund.getStatus().toString();
        this.amount = convertToRupees(refund.getRefundAmountInPaise());
    }
}
```

---

### Exception.template.java

**When to use**: Creating custom exceptions for error handling

**Exception Hierarchy**:
```
RuntimeException (Unchecked, causes rollback)
└── ApplicationException (Base)
    ├── BadRequestException (400)
    ├── ResourceNotFoundException (404)
    ├── InvalidStateException (409)
    ├── DuplicateResourceException (409)
    ├── BusinessException (422)
    ├── ExternalServiceException (502)
    └── InternalServerException (500)
```

**Key sections**:
- Custom exception classes
- HTTP status codes
- Error codes (machine-readable)
- Global exception handler

**After copying**:
1. Add specific exception classes for your domain
2. Implement @ExceptionHandler methods
3. Return ErrorResponse

**Example**:
```java
// In RefundService:
if (refundAmount > payment.getAmountInPaise()) {
    throw new BadRequestException("Refund amount > payment amount");
}

// Caught by GlobalExceptionHandler:
@ExceptionHandler(BadRequestException.class)
public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
}
```

---

### Test.template.java

**When to use**: Creating unit tests for service layer

**Test Levels**:
1. **Unit Tests** (mocked): Fast, isolated, focused
2. **Integration Tests**: Full Spring context, real DB
3. **E2E Tests**: HTTP layer, MockMvc

**Test Pattern**:
```
Given (setup) → When (action) → Then (assert)
```

**Key sections**:
- Setup with mocks
- Happy path tests
- Error case tests
- Edge case tests
- Expiry logic tests
- Fee calculation tests

**After copying**:
1. Configure mocks in `setUp()`
2. Add test cases for each business path
3. Test both success and failure scenarios

**Example**:
```java
@Test
public void testCreateRefund_WithValidPayment_ReturnsRefund() {
    // Given
    Payment payment = new Payment();
    payment.setStatus(PaymentStatus.SUCCESS);
    payment.setAmountInPaise(25000);
    when(paymentRepository.findByPaymentUuid("..."))
        .thenReturn(Optional.of(payment));

    // When
    RefundOrder refund = refundService.createRefund("...", 5000, "reason");

    // Then
    assertNotNull(refund);
    assertEquals(5000, refund.getRefundAmountInPaise());
}
```

---

## Common Patterns

### Pattern 1: Entity with Relationships

**One-to-Many (Merchant has many PaymentOrders)**

```java
@Entity
public class Merchant {
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.NONE)
    private List<PaymentOrder> payments = new ArrayList<>();
}

@Entity
public class PaymentOrder {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;
}
```

### Pattern 2: State Machine

```java
private void validateTransition(Status from, Status to) {
    if (from.equals(Status.CREATED)) {
        if (!List.of(Status.PENDING, Status.CANCELLED).contains(to)) {
            throw new InvalidStateException("Invalid transition");
        }
    }
}
```

### Pattern 3: Amount Handling

```java
// Input: ₹250.50
// Storage: 25050 (paise, BIGINT)
// Output: 250.50 (BigDecimal, for API)

long amountInPaise = 25050;
BigDecimal rupees = BigDecimal.valueOf(amountInPaise)
    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);  // 250.50
```

### Pattern 4: Pagination

```java
Page<PaymentOrder> page = paymentOrderRepository.findByMerchantAndStatus(
    merchantId,
    PaymentStatus.SUCCESS,
    PageRequest.of(0, 20)  // Page 0, 20 items
);

// Response
{
  "content": [...],
  "total_elements": 42,
  "total_pages": 3,
  "current_page": 0,
  "page_size": 20
}
```

---

## Folder Structure

```
doc/scaffolds/
├── README.md                    # This file
├── Entity.template.java
├── Repository.template.java
├── Service.template.java
├── Controller.template.java
├── DTOs.template.java
├── Exception.template.java
└── Test.template.java
```

---

## Tips & Tricks

### Bulk Replace in VS Code

```
Ctrl+H (Find & Replace)

Find:     [ENTITY_NAME]
Replace:  RefundOrder
Replace All (Ctrl+Alt+Enter)

Find:     [table_name]
Replace:  refundOrder
Replace All

Find:     [TABLE_NAME]
Replace:  refund_order
Replace All
```

### Generate Getters/Setters (IntelliJ)

```
1. Right-click on entity class
2. Generate → Getters and Setters
3. Select all fields
4. OK
```

### Test Entity with TestEntityManager

```java
@DataJpaTest
public class RefundOrderRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testFindByUuid() {
        RefundOrder refund = new RefundOrder(...);
        entityManager.persistAndFlush(refund);

        RefundOrder found = repository.findByRefundOrderUuid(refund.getRefundOrderUuid());
        assertEquals(refund.getId(), found.getId());
    }
}
```

### Run Single Test Method

```bash
mvn test -Dtest=RefundServiceTest#testCreateRefund_WithValidPayment_ReturnsRefund
```

### Debug Test with Print Statements

```java
// Add in test
System.out.println("Refund amount: " + refund.getRefundAmountInPaise());

// Run with output
mvn test -Dtest=RefundServiceTest -X
```

---

## Related Documentation

- **[README.md](../README.md)** - Project overview & quick start
- **[DEVELOPER_GUIDE.md](../DEVELOPER_GUIDE.md)** - Architecture patterns & best practices
- **[DOMAIN_MODEL.md](../DOMAIN_MODEL.md)** - Entity definitions
- **[DATABASE_SCHEMA.md](../DATABASE_SCHEMA.md)** - DDL & indexes
- **[IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md)** - Step-by-step roadmap

---

## Support

**Questions about templates?**
- Check JavaDoc in each template file
- See "Common Patterns" section above
- Refer to [DEVELOPER_GUIDE.md](../DEVELOPER_GUIDE.md) for architecture

**Stuck on implementation?**
- Look at existing entities (Payment, Merchant, etc.)
- Run tests: `mvn test`
- Check error messages and logs

---

**Last Updated**: 2026-01-10
**Version**: Phase-1.0
