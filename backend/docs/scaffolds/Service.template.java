package org.personal.service;

import lombok.extern.slf4j.Slf4j;
import org.personal.entity.[ENTITY_NAME];
import org.personal.entity.Merchant;
import org.personal.exception.BadRequestException;
import org.personal.exception.InvalidStateException;
import org.personal.exception.ResourceNotFoundException;
import org.personal.repository.[ENTITY_NAME]Repository;
import org.personal.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * [ENTITY_NAME]Service - Business logic for [ENTITY_NAME]
 *
 * Responsibilities:
 * 1. Validate preconditions (merchant exists, is active, etc.)
 * 2. Perform state transitions (CREATED → PENDING → SUCCESS)
 * 3. Calculate derived fields (fees, net amounts)
 * 4. Record events (audit trail)
 * 5. Manage relationships (create/update related entities)
 *
 * Never call Repository directly from Controller
 * Always use this Service layer for business logic
 *
 * Usage:
 * 1. Copy this file: cp Service.template.java [ENTITY_NAME]Service.java
 * 2. Replace [ENTITY_NAME] with your entity name
 * 3. Implement create/get/list/update methods
 * 4. Add @Transactional for atomicity
 */
@Slf4j
@Service
@Transactional
public class [ENTITY_NAME]Service {

    // ==================== Dependencies ====================

    @Autowired
    private [ENTITY_NAME]Repository [table_name]Repository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaymentEventService paymentEventService;  // For audit trail

    @Autowired
    private Clock clock;  // Mockable for testing

    // ==================== Create Operations ====================

    /**
     * Create new [ENTITY_NAME]
     *
     * Preconditions:
     * - Merchant exists and is ACTIVE
     * - Amount is positive
     *
     * Actions:
     * 1. Validate preconditions
     * 2. Calculate derived fields
     * 3. Create entity
     * 4. Persist to database
     * 5. Record event (audit trail)
     * 6. Return entity
     */
    public [ENTITY_NAME] create(String merchantId, BigDecimal amount, String description) {
        log.info("Creating [ENTITY_NAME]: merchant={}, amount={}", merchantId, amount);

        // 1. Validate preconditions
        Merchant merchant = merchantRepository.findByMerchantUuid(merchantId)
            .orElseThrow(() -> {
                log.error("Merchant not found: {}", merchantId);
                return new ResourceNotFoundException("Merchant not found: " + merchantId);
            });

        if (!merchant.getStatus().equals("ACTIVE")) {
            throw new BusinessException("Merchant is not active");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Amount must be > 0");
        }

        // 2. Calculate derived fields
        long amountInPaise = convertToPaise(amount);
        long platformFeeInPaise = calculatePlatformFee(amountInPaise, merchant.getPlatformFeePercentage());
        long merchantNetInPaise = amountInPaise - platformFeeInPaise;

        // 3. Create entity
        [ENTITY_NAME] entity = new [ENTITY_NAME]();
        entity.set[ENTITY_NAME]Uuid(UUID.randomUUID().toString());
        entity.setMerchant(merchant);
        entity.setAmountInPaise(amountInPaise);
        entity.setStatus([ENUM_STATUS].CREATED);
        entity.setExpiresAt(Instant.now(clock).plus(15, ChronoUnit.MINUTES));
        entity.setPlatformFeeInPaise(platformFeeInPaise);
        entity.setMerchantNetInPaise(merchantNetInPaise);
        entity.setCurrency("INR");
        entity.setDescription(description);
        entity.setCreatedAt(Instant.now(clock));
        entity.setUpdatedAt(Instant.now(clock));

        // 4. Persist
        [ENTITY_NAME] saved = [table_name]Repository.save(entity);

        // 5. Record event (audit trail)
        paymentEventService.recordEvent(
            saved,
            PaymentEventType.CREATED,
            Map.of("merchantId", merchantId, "amount", amount.toString())
        );

        log.info("Created [ENTITY_NAME]: uuid={}", saved.get[ENTITY_NAME]Uuid());
        return saved;
    }

    // ==================== Read Operations ====================

    /**
     * Get [ENTITY_NAME] by UUID
     * Throws ResourceNotFoundException if not found
     */
    public [ENTITY_NAME] getByUuid(String uuid) {
        return [table_name]Repository.findBy[ENTITY_NAME]Uuid(uuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                "[ENTITY_NAME] not found: " + uuid
            ));
    }

    /**
     * Get [ENTITY_NAME] by UUID with optional expiry check
     * If expired, marks as EXPIRED and returns updated entity
     */
    public [ENTITY_NAME] getByUuidWithExpiryCheck(String uuid) {
        [ENTITY_NAME] entity = getByUuid(uuid);

        // Check expiry (on-read evaluation)
        if (isExpired(entity)) {
            entity.setStatus([ENUM_STATUS].EXPIRED);
            entity.setUpdatedAt(Instant.now(clock));
            entity = [table_name]Repository.save(entity);
            log.info("[ENTITY_NAME] marked as EXPIRED: uuid={}", uuid);
        }

        return entity;
    }

    /**
     * List [ENTITY_NAME] by merchant and status (paginated)
     */
    public Page<[ENTITY_NAME]> listByMerchantAndStatus(
        String merchantId,
        [ENUM_STATUS] status,
        int page,
        int size) {

        // Find merchant to get internal ID
        Merchant merchant = merchantRepository.findByMerchantUuid(merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

        Pageable pageable = PageRequest.of(page, size);
        return [table_name]Repository.findByMerchantAndStatus(
            merchant.getId(),
            status,
            pageable
        );
    }

    // ==================== Update Operations ====================

    /**
     * Transition [ENTITY_NAME] status
     * Validates state machine rules
     */
    public [ENTITY_NAME] transitionStatus(String uuid, [ENUM_STATUS] newStatus) {
        [ENTITY_NAME] entity = getByUuid(uuid);

        // Validate transition
        validateTransition(entity.getStatus(), newStatus);

        // Update status
        entity.setStatus(newStatus);
        entity.setUpdatedAt(Instant.now(clock));
        [ENTITY_NAME] saved = [table_name]Repository.save(entity);

        // Record event
        paymentEventService.recordEvent(
            saved,
            PaymentEventType.valueOf(newStatus.toString()),
            Map.of("previousStatus", entity.getStatus().toString())
        );

        log.info("[ENTITY_NAME] transitioned: uuid={}, from={}, to={}",
            uuid, entity.getStatus(), newStatus);
        return saved;
    }

    /**
     * Soft delete [ENTITY_NAME]
     */
    public void delete(String uuid) {
        [ENTITY_NAME] entity = getByUuid(uuid);
        entity.setDeletedAt(Instant.now(clock));
        [table_name]Repository.save(entity);
        log.info("[ENTITY_NAME] soft-deleted: uuid={}", uuid);
    }

    // ==================== Business Logic ====================

    /**
     * Calculate platform fee
     * Formula: amount * feePercentage / 100
     * Always rounds using HALF_UP (banker's rounding)
     */
    private long calculatePlatformFee(long amountInPaise, BigDecimal feePercentage) {
        BigDecimal feeAmount = BigDecimal.valueOf(amountInPaise)
            .multiply(feePercentage)
            .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        return feeAmount.longValue();
    }

    /**
     * Convert rupees (BigDecimal) to paise (BIGINT)
     * Example: ₹100.50 → 10050 paise
     */
    private long convertToPaise(BigDecimal rupees) {
        return rupees.multiply(BigDecimal.valueOf(100))
            .longValue();
    }

    /**
     * Check if entity has expired
     */
    private boolean isExpired([ENTITY_NAME] entity) {
        return Instant.now(clock).isAfter(entity.getExpiresAt());
    }

    /**
     * Validate state transition (state machine)
     * Throws InvalidStateException if transition is not allowed
     */
    private void validateTransition([ENUM_STATUS] from, [ENUM_STATUS] to) {
        // Define valid transitions
        // Example:
        // CREATED → QR_GENERATED, CANCELLED
        // QR_GENERATED → PENDING, EXPIRED, CANCELLED
        // PENDING → SUCCESS, FAILED, EXPIRED
        // SUCCESS/FAILED/EXPIRED/CANCELLED → (terminal, no transitions)

        if (from.equals([ENUM_STATUS].CREATED)) {
            if (!List.of([ENUM_STATUS].QR_GENERATED, [ENUM_STATUS].CANCELLED).contains(to)) {
                throw new InvalidStateException(
                    "[ENTITY_NAME] in CREATED state can only transition to QR_GENERATED or CANCELLED"
                );
            }
        } else if (from.equals([ENUM_STATUS].SUCCESS) || from.equals([ENUM_STATUS].FAILED) ||
                   from.equals([ENUM_STATUS].EXPIRED) || from.equals([ENUM_STATUS].CANCELLED)) {
            throw new InvalidStateException(
                "[ENTITY_NAME] in " + from + " state (terminal) cannot transition further"
            );
        }
        // ... add more transition rules
    }

    // ==================== Batch Operations ====================

    /**
     * Find and mark expired [ENTITY_NAME]s
     * Called by scheduled job or on-read check
     */
    public void markExpiredEntities() {
        List<[ENTITY_NAME]> expired = [table_name]Repository.findExpiredEntities(Instant.now(clock));
        log.info("Found {} expired entities", expired.size());

        for ([ENTITY_NAME] entity : expired) {
            entity.setStatus([ENUM_STATUS].EXPIRED);
            entity.setUpdatedAt(Instant.now(clock));
            [table_name]Repository.save(entity);

            paymentEventService.recordEvent(
                entity,
                PaymentEventType.EXPIRED,
                Map.of("expiredAt", Instant.now(clock).toString())
            );
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Check if [ENTITY_NAME] exists
     */
    public boolean existsByUuid(String uuid) {
        return [table_name]Repository.existsBy[ENTITY_NAME]Uuid(uuid);
    }
}
