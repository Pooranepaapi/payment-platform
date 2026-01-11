package org.personal.controller;

import lombok.extern.slf4j.Slf4j;
import org.personal.dto.Create[ENTITY_NAME]Request;
import org.personal.dto.[ENTITY_NAME]Response;
import org.personal.entity.[ENTITY_NAME];
import org.personal.service.[ENTITY_NAME]Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for [ENTITY_NAME]
 *
 * Design Principles:
 * 1. HTTP Semantics:
 *    - POST (201 Created): Create new resource
 *    - GET (200 OK): Retrieve resource
 *    - PUT (200 OK): Update resource
 *    - DELETE (204 No Content): Delete resource
 *    - PATCH (200 OK): Partial update
 *
 * 2. Never expose entities directly in API responses (use DTOs)
 *
 * 3. Input validation happens here, business logic in Service
 *
 * 4. Error handling via @ExceptionHandler (centralized)
 *
 * Usage:
 * 1. Copy this file: cp Controller.template.java [ENTITY_NAME]Controller.java
 * 2. Replace [ENTITY_NAME] with your entity name
 * 3. Replace [API_PATH] with your API path (e.g., /api/v1/[entities])
 * 4. Implement create/get/list/update endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/[API_PATH]")
@CrossOrigin(origins = "*")  // Enable CORS if frontend is different domain
public class [ENTITY_NAME]Controller {

    // ==================== Dependencies ====================

    @Autowired
    private [ENTITY_NAME]Service [table_name]Service;

    // ==================== POST: Create ====================

    /**
     * POST /api/v1/[entities]
     * Create new [ENTITY_NAME]
     *
     * @param request Contains: amount, description, merchantId
     * @return 201 Created with [ENTITY_NAME] UUID
     *
     * Example:
     * POST /api/v1/payments
     * {
     *   "merchantId": "MER001",
     *   "amount": 250.00,
     *   "description": "Coffee purchase"
     * }
     *
     * Response:
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440001",
     *   "status": "CREATED",
     *   "amount": 250.00,
     *   "expiresAt": "2026-01-10T14:30:00Z"
     * }
     */
    @PostMapping
    public ResponseEntity<[ENTITY_NAME]Response> create(
        @RequestBody Create[ENTITY_NAME]Request request) {

        log.info("Creating [ENTITY_NAME]: merchantId={}, amount={}",
            request.getMerchantId(), request.getAmount());

        // 1. Validate input
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new BadRequestException("Amount must be > 0");
        }

        // 2. Call service (business logic)
        [ENTITY_NAME] entity = [table_name]Service.create(
            request.getMerchantId(),
            request.getAmount(),
            request.getDescription()
        );

        // 3. Convert entity to DTO
        [ENTITY_NAME]Response response = new [ENTITY_NAME]Response(entity);

        // 4. Return with 201 status
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== GET: Retrieve ====================

    /**
     * GET /api/v1/[entities]/{id}
     * Get single [ENTITY_NAME] by UUID
     *
     * @param id [ENTITY_NAME] UUID
     * @return 200 OK with [ENTITY_NAME] details
     *
     * Example:
     * GET /api/v1/payments/550e8400-e29b-41d4-a716-446655440001
     *
     * Response:
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440001",
     *   "status": "PENDING",
     *   "amount": 250.00,
     *   "expiresAt": "2026-01-10T14:30:00Z"
     * }
     *
     * Error:
     * 404 Not Found: [ENTITY_NAME] not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<[ENTITY_NAME]Response> get(@PathVariable String id) {
        log.info("Getting [ENTITY_NAME]: id={}", id);

        // 1. Validate UUID format (optional, Spring does this)
        if (id == null || id.isBlank()) {
            throw new BadRequestException("[ENTITY_NAME] ID cannot be empty");
        }

        // 2. Call service (with expiry check)
        [ENTITY_NAME] entity = [table_name]Service.getByUuidWithExpiryCheck(id);

        // 3. Convert to DTO
        [ENTITY_NAME]Response response = new [ENTITY_NAME]Response(entity);

        // 4. Return
        return ResponseEntity.ok(response);
    }

    // ==================== GET: List (Paginated) ====================

    /**
     * GET /api/v1/[entities]?merchant={merchantId}&status={status}&page={page}&size={size}
     * List [ENTITY_NAME]s with filters and pagination
     *
     * @param merchantId Filter by merchant UUID
     * @param status Filter by status (CREATED, PENDING, SUCCESS, etc.)
     * @param page Page number (0-indexed)
     * @param size Page size (default 20)
     * @return 200 OK with paginated results
     *
     * Example:
     * GET /api/v1/payments?merchant=MER001&status=SUCCESS&page=0&size=10
     *
     * Response:
     * {
     *   "content": [
     *     { "id": "...", "status": "SUCCESS", "amount": 250.00 },
     *     ...
     *   ],
     *   "totalElements": 42,
     *   "totalPages": 5,
     *   "currentPage": 0,
     *   "pageSize": 10
     * }
     */
    @GetMapping
    public ResponseEntity<PageResponse<[ENTITY_NAME]Response>> list(
        @RequestParam(required = false) String merchant,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        log.info("Listing [ENTITY_NAME]s: merchant={}, status={}, page={}, size={}",
            merchant, status, page, size);

        // 1. Validate pagination parameters
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination: page >= 0, 0 < size <= 100");
        }

        // 2. Call service with filters
        Page<[ENTITY_NAME]> entities = [table_name]Service.listByMerchantAndStatus(
            merchant,
            status != null ? [ENUM_STATUS].valueOf(status) : null,
            page,
            size
        );

        // 3. Convert to DTOs
        List<[ENTITY_NAME]Response> content = entities.getContent()
            .stream()
            .map([ENTITY_NAME]Response::new)
            .collect(Collectors.toList());

        // 4. Wrap in pagination response
        PageResponse<[ENTITY_NAME]Response> response = new PageResponse<>(
            content,
            entities.getTotalElements(),
            entities.getTotalPages(),
            page,
            size
        );

        return ResponseEntity.ok(response);
    }

    // ==================== POST: State Transitions ====================

    /**
     * POST /api/v1/[entities]/{id}/transition
     * Transition [ENTITY_NAME] status
     *
     * @param id [ENTITY_NAME] UUID
     * @param newStatus Target status
     * @return 200 OK with updated [ENTITY_NAME]
     *
     * Valid transitions are enforced by service (state machine)
     *
     * Example:
     * POST /api/v1/payments/550e8400-e29b-41d4-a716-446655440001/transition?status=SUCCESS
     *
     * Response:
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440001",
     *   "status": "SUCCESS",
     *   "amount": 250.00
     * }
     *
     * Error:
     * 400 Bad Request: Invalid state transition
     */
    @PostMapping("/{id}/transition")
    public ResponseEntity<[ENTITY_NAME]Response> transition(
        @PathVariable String id,
        @RequestParam String newStatus) {

        log.info("Transitioning [ENTITY_NAME]: id={}, newStatus={}", id, newStatus);

        // 1. Validate status enum
        [ENUM_STATUS] status;
        try {
            status = [ENUM_STATUS].valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + newStatus);
        }

        // 2. Call service (validates state machine)
        [ENTITY_NAME] entity = [table_name]Service.transitionStatus(id, status);

        // 3. Convert to DTO
        [ENTITY_NAME]Response response = new [ENTITY_NAME]Response(entity);

        return ResponseEntity.ok(response);
    }

    // ==================== DELETE: Soft Delete ====================

    /**
     * DELETE /api/v1/[entities]/{id}
     * Soft delete [ENTITY_NAME] (mark as deleted, don't remove from DB)
     *
     * @param id [ENTITY_NAME] UUID
     * @return 204 No Content
     *
     * Example:
     * DELETE /api/v1/payments/550e8400-e29b-41d4-a716-446655440001
     *
     * Response:
     * 204 No Content
     *
     * Note: Data is not removed from database, just marked with deletedAt timestamp
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Deleting [ENTITY_NAME]: id={}", id);

        [table_name]Service.delete(id);

        return ResponseEntity.noContent().build();
    }

    // ==================== Helper Methods ====================

    /**
     * Convert entity to response DTO
     * Keep this helper if you need custom conversion logic
     */
    private [ENTITY_NAME]Response toResponse([ENTITY_NAME] entity) {
        return new [ENTITY_NAME]Response(
            entity.get[ENTITY_NAME]Uuid(),
            entity.getStatus().toString(),
            convertToRupees(entity.getAmountInPaise()),
            entity.getExpiresAt()
        );
    }

    /**
     * Convert paise (BIGINT) to rupees (BigDecimal)
     */
    private BigDecimal convertToRupees(long paise) {
        return BigDecimal.valueOf(paise).divide(
            BigDecimal.valueOf(100),
            2,
            RoundingMode.HALF_UP
        );
    }
}
