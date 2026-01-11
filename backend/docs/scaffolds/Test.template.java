package org.personal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.personal.entity.[ENTITY_NAME];
import org.personal.entity.Merchant;
import org.personal.exception.BadRequestException;
import org.personal.exception.ResourceNotFoundException;
import org.personal.exception.InvalidStateException;
import org.personal.repository.[ENTITY_NAME]Repository;
import org.personal.repository.MerchantRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for [ENTITY_NAME]Service
 *
 * Testing Strategy:
 * 1. Unit tests (with mocks) - Fast, isolated, focused
 * 2. Integration tests (@SpringBootTest) - Full context, database
 * 3. E2E tests (@SpringBootTest + MockMvc) - HTTP layer
 *
 * This file contains UNIT TESTS (mocked dependencies)
 * For INTEGRATION tests, see [ENTITY_NAME]ServiceIntegrationTest
 *
 * Mocking Pattern:
 * - Mock repositories
 * - Inject mocks into service
 * - Test business logic in isolation
 * - Don't test Spring Data queries here
 *
 * Test Naming Convention:
 * - testMethodName_WithCondition_ExpectedResult()
 * - Example: testCreate_WithValidInput_ReturnsEntity()
 *
 * Assertion Pattern:
 * - Given (setup): Create test data, mock responses
 * - When (action): Call service method
 * - Then (assert): Verify output, side effects, mock calls
 *
 * Usage:
 * 1. Copy this file: cp Test.template.java [ENTITY_NAME]ServiceTest.java
 * 2. Replace [ENTITY_NAME] with your entity name
 * 3. Add test methods for each business logic path
 * 4. Run: mvn test -Dtest=[ENTITY_NAME]ServiceTest
 */
@DisplayName("[ENTITY_NAME]Service Unit Tests")
public class [ENTITY_NAME]ServiceTest {

    // ==================== Setup ====================

    /**
     * Service under test
     */
    private [ENTITY_NAME]Service service;

    /**
     * Mocked dependencies
     */
    @Mock
    private [ENTITY_NAME]Repository [table_name]Repository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PaymentEventService paymentEventService;

    @Mock
    private Clock clock;

    /**
     * Run before each test
     * Initializes mocks and service instance
     */
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        service = new [ENTITY_NAME]Service();
        service.[table_name]Repository = [table_name]Repository;
        service.merchantRepository = merchantRepository;
        service.paymentEventService = paymentEventService;
        service.clock = clock;
    }

    // ==================== Tests: Create ====================

    /**
     * Test: Create [ENTITY_NAME] with valid input
     *
     * Given:
     * - Valid merchant (exists, is active)
     * - Valid amount (positive)
     *
     * When:
     * - Call create(merchantId, amount, description)
     *
     * Then:
     * - Returns [ENTITY_NAME] entity
     * - Entity has UUID, status=CREATED, correct amounts
     * - Repository.save() called once
     * - Event recorded
     */
    @Test
    @DisplayName("create() with valid input should return saved entity")
    public void testCreate_WithValidInput_ReturnsSavedEntity() {
        // Given
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setMerchantUuid("MER001");
        merchant.setName("Test Store");
        merchant.setStatus("ACTIVE");
        merchant.setPlatformFeePercentage(BigDecimal.valueOf(2.5));

        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        when(clock.instant()).thenReturn(now);
        when(merchantRepository.findByMerchantUuid("MER001"))
            .thenReturn(Optional.of(merchant));

        [ENTITY_NAME] savedEntity = new [ENTITY_NAME]();
        savedEntity.setId(1L);
        savedEntity.set[ENTITY_NAME]Uuid("550e8400-e29b-41d4-a716-446655440001");
        when([table_name]Repository.save(any([ENTITY_NAME].class)))
            .thenReturn(savedEntity);

        // When
        [ENTITY_NAME] result = service.create("MER001", BigDecimal.valueOf(250), "Test payment");

        // Then
        assertNotNull(result);
        assertNotNull(result.get[ENTITY_NAME]Uuid());
        assertEquals("CREATED", result.getStatus().toString());
        assertEquals(25000, result.getAmountInPaise());  // ₹250 = 25000 paise
        assertEquals(625, result.getPlatformFeeInPaise());  // 2.5% of 25000
        assertEquals(24375, result.getMerchantNetInPaise());  // 25000 - 625

        verify([table_name]Repository, times(1)).save(any([ENTITY_NAME].class));
        verify(paymentEventService, times(1)).recordEvent(any(), any(), any());
    }

    /**
     * Test: Create fails with invalid merchant
     *
     * Given:
     * - Merchant doesn't exist
     *
     * When:
     * - Call create() with invalid merchantId
     *
     * Then:
     * - Throws ResourceNotFoundException
     * - Repository.save() not called
     */
    @Test
    @DisplayName("create() with invalid merchant should throw ResourceNotFoundException")
    public void testCreate_WithInvalidMerchant_ThrowsResourceNotFoundException() {
        // Given
        when(merchantRepository.findByMerchantUuid("INVALID"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            service.create("INVALID", BigDecimal.valueOf(250), "Test");
        });

        verify([table_name]Repository, never()).save(any());
    }

    /**
     * Test: Create fails with inactive merchant
     *
     * Given:
     * - Merchant exists but is INACTIVE
     *
     * When:
     * - Call create()
     *
     * Then:
     * - Throws BusinessException
     */
    @Test
    @DisplayName("create() with inactive merchant should throw BusinessException")
    public void testCreate_WithInactiveMerchant_ThrowsBusinessException() {
        // Given
        Merchant merchant = new Merchant();
        merchant.setStatus("INACTIVE");
        when(merchantRepository.findByMerchantUuid("MER001"))
            .thenReturn(Optional.of(merchant));

        // When & Then
        assertThrows(BusinessException.class, () -> {
            service.create("MER001", BigDecimal.valueOf(250), "Test");
        });

        verify([table_name]Repository, never()).save(any());
    }

    /**
     * Test: Create fails with negative amount
     *
     * Given:
     * - Valid merchant
     * - Negative amount
     *
     * When:
     * - Call create()
     *
     * Then:
     * - Throws BadRequestException
     */
    @Test
    @DisplayName("create() with negative amount should throw BadRequestException")
    public void testCreate_WithNegativeAmount_ThrowsBadRequestException() {
        // Given
        Merchant merchant = new Merchant();
        merchant.setStatus("ACTIVE");
        when(merchantRepository.findByMerchantUuid("MER001"))
            .thenReturn(Optional.of(merchant));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            service.create("MER001", BigDecimal.valueOf(-100), "Test");
        });

        verify([table_name]Repository, never()).save(any());
    }

    // ==================== Tests: Get ====================

    /**
     * Test: Get [ENTITY_NAME] by UUID
     *
     * Given:
     * - [ENTITY_NAME] exists
     *
     * When:
     * - Call getByUuid(uuid)
     *
     * Then:
     * - Returns entity
     */
    @Test
    @DisplayName("getByUuid() should return entity when found")
    public void testGetByUuid_WhenFound_ReturnsEntity() {
        // Given
        [ENTITY_NAME] entity = new [ENTITY_NAME]();
        entity.setId(1L);
        entity.set[ENTITY_NAME]Uuid("550e8400-e29b-41d4-a716-446655440001");
        when([table_name]Repository.findBy[ENTITY_NAME]Uuid("550e8400-e29b-41d4-a716-446655440001"))
            .thenReturn(Optional.of(entity));

        // When
        [ENTITY_NAME] result = service.getByUuid("550e8400-e29b-41d4-a716-446655440001");

        // Then
        assertNotNull(result);
        assertEquals("550e8400-e29b-41d4-a716-446655440001", result.get[ENTITY_NAME]Uuid());
    }

    /**
     * Test: Get [ENTITY_NAME] fails when not found
     *
     * Given:
     * - [ENTITY_NAME] doesn't exist
     *
     * When:
     * - Call getByUuid()
     *
     * Then:
     * - Throws ResourceNotFoundException
     */
    @Test
    @DisplayName("getByUuid() should throw ResourceNotFoundException when not found")
    public void testGetByUuid_WhenNotFound_ThrowsResourceNotFoundException() {
        // Given
        when([table_name]Repository.findBy[ENTITY_NAME]Uuid("INVALID"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            service.getByUuid("INVALID");
        });
    }

    // ==================== Tests: State Transitions ====================

    /**
     * Test: Transition status from CREATED to QR_GENERATED
     *
     * Given:
     * - [ENTITY_NAME] in CREATED state
     *
     * When:
     * - Call transitionStatus(uuid, QR_GENERATED)
     *
     * Then:
     * - Status changed to QR_GENERATED
     * - Repository.save() called
     * - Event recorded
     */
    @Test
    @DisplayName("transitionStatus() should update entity status")
    public void testTransitionStatus_WithValidTransition_UpdatesStatus() {
        // Given
        [ENTITY_NAME] entity = new [ENTITY_NAME]();
        entity.setId(1L);
        entity.set[ENTITY_NAME]Uuid("550e8400-...");
        entity.setStatus([ENUM_STATUS].CREATED);

        when([table_name]Repository.findBy[ENTITY_NAME]Uuid("550e8400-..."))
            .thenReturn(Optional.of(entity));
        when([table_name]Repository.save(any()))
            .thenReturn(entity);

        // When
        service.transitionStatus("550e8400-...", [ENUM_STATUS].QR_GENERATED);

        // Then
        assertEquals([ENUM_STATUS].QR_GENERATED, entity.getStatus());
        verify([table_name]Repository, times(1)).save(any());
        verify(paymentEventService, times(1)).recordEvent(any(), any(), any());
    }

    /**
     * Test: Transition fails with invalid state transition
     *
     * Given:
     * - [ENTITY_NAME] in SUCCESS state (terminal)
     *
     * When:
     * - Try to transition to PENDING (invalid)
     *
     * Then:
     * - Throws InvalidStateException
     */
    @Test
    @DisplayName("transitionStatus() should throw InvalidStateException for invalid transition")
    public void testTransitionStatus_WithInvalidTransition_ThrowsInvalidStateException() {
        // Given
        [ENTITY_NAME] entity = new [ENTITY_NAME]();
        entity.setStatus([ENUM_STATUS].SUCCESS);  // Terminal state

        when([table_name]Repository.findBy[ENTITY_NAME]Uuid("550e8400-..."))
            .thenReturn(Optional.of(entity));

        // When & Then
        assertThrows(InvalidStateException.class, () -> {
            service.transitionStatus("550e8400-...", [ENUM_STATUS].PENDING);
        });

        verify([table_name]Repository, never()).save(any());
    }

    // ==================== Tests: Expiry Logic ====================

    /**
     * Test: Expiry check marks payment as EXPIRED
     *
     * Given:
     * - [ENTITY_NAME] with expiresAt = now - 5 minutes
     * - Clock is mocked to return "now"
     *
     * When:
     * - Call getByUuidWithExpiryCheck()
     *
     * Then:
     * - Status changed to EXPIRED
     * - Repository.save() called
     */
    @Test
    @DisplayName("getByUuidWithExpiryCheck() should mark expired entity")
    public void testGetByUuidWithExpiryCheck_WhenExpired_MarksAsExpired() {
        // Given
        Instant now = Instant.parse("2026-01-10T10:15:00Z");
        Instant expiredAt = Instant.parse("2026-01-10T10:10:00Z");  // 5 min ago

        [ENTITY_NAME] entity = new [ENTITY_NAME]();
        entity.set[ENTITY_NAME]Uuid("550e8400-...");
        entity.setExpiresAt(expiredAt);
        entity.setStatus([ENUM_STATUS].PENDING);

        when(clock.instant()).thenReturn(now);
        when([table_name]Repository.findBy[ENTITY_NAME]Uuid("550e8400-..."))
            .thenReturn(Optional.of(entity));
        when([table_name]Repository.save(any()))
            .thenReturn(entity);

        // When
        [ENTITY_NAME] result = service.getByUuidWithExpiryCheck("550e8400-...");

        // Then
        assertEquals([ENUM_STATUS].EXPIRED, result.getStatus());
        verify([table_name]Repository, times(1)).save(any());
    }

    // ==================== Tests: Fee Calculation ====================

    /**
     * Test: Platform fee calculated correctly
     *
     * Scenario: ₹250 with 2.5% fee
     * - Amount: 250.00 (₹)
     * - AmountInPaise: 25000
     * - Fee%: 2.5
     * - FeeInPaise: 25000 * 2.5 / 100 = 625 paise (₹6.25)
     * - MerchantNet: 25000 - 625 = 24375 paise (₹243.75)
     */
    @Test
    @DisplayName("Fee calculation should be accurate")
    public void testFeeCalculation_WithAmount250AndFee2_5_CalculatesCorrectly() {
        // Given
        Merchant merchant = new Merchant();
        merchant.setPlatformFeePercentage(BigDecimal.valueOf(2.5));
        when(merchantRepository.findByMerchantUuid("MER001"))
            .thenReturn(Optional.of(merchant));
        when([table_name]Repository.save(any()))
            .thenReturn(new [ENTITY_NAME]());

        // When
        service.create("MER001", BigDecimal.valueOf(250), "Test");

        // Then
        // Verify that save was called with correct amounts
        // (Details depend on implementation - adjust as needed)
    }
}

// ==================== Integration Tests ====================

/**
 * Integration Tests for [ENTITY_NAME]Service
 *
 * Location: org.personal.service.[ENTITY_NAME]ServiceIntegrationTest
 *
 * Use @SpringBootTest to load full context:
 *
 * @SpringBootTest
 * @Transactional  // Rolls back after each test
 * public class [ENTITY_NAME]ServiceIntegrationTest {
 *
 *     @Autowired
 *     private [ENTITY_NAME]Service service;
 *
 *     @Autowired
 *     private [ENTITY_NAME]Repository repository;
 *
 *     @Autowired
 *     private MerchantRepository merchantRepository;
 *
 *     @Test
 *     public void testCreateAndRetrieve_EndToEnd() {
 *         // Create merchant
 *         Merchant merchant = new Merchant(...);
 *         merchantRepository.save(merchant);
 *
 *         // Create entity
 *         [ENTITY_NAME] entity = service.create(merchant.getMerchantUuid(), ...);
 *
 *         // Verify persisted
 *         [ENTITY_NAME] retrieved = repository.findBy[ENTITY_NAME]Uuid(entity.get[ENTITY_NAME]Uuid())
 *             .orElseThrow();
 *
 *         assertEquals(entity.getStatus(), retrieved.getStatus());
 *     }
 * }
 */
