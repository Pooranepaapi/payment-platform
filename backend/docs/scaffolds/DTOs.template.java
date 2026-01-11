package org.personal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.personal.entity.[ENTITY_NAME];

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO Templates for [ENTITY_NAME]
 *
 * DTOs (Data Transfer Objects) are used for:
 * 1. API Contract: Define what frontend/external systems see
 * 2. Versioning: Can change DTO without breaking entity
 * 3. Security: Hide internal fields (createdAt, updatedAt, etc.)
 * 4. Serialization: Control JSON field names and formats
 *
 * Rules:
 * - Never expose JPA entities directly in API responses
 * - Use DTOs for both request and response
 * - Use @JsonProperty to map JSON field names (camelCase API vs snake_case DB)
 * - Use BigDecimal for money (never double/float)
 * - Always include JavaDoc for fields
 */

// ==================== REQUEST DTOs ====================

/**
 * Create[ENTITY_NAME]Request
 * Request body for POST /api/v1/[entities]
 *
 * Example:
 * {
 *   "merchantId": "MER001",
 *   "amount": 250.00,
 *   "description": "Coffee purchase"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Create[ENTITY_NAME]Request {

    /**
     * Merchant UUID (external reference)
     */
    @JsonProperty("merchant_id")
    private String merchantId;

    /**
     * Amount in rupees (BigDecimal for precision)
     * Backend will convert to paise (BIGINT)
     */
    private BigDecimal amount;

    /**
     * Optional: Payment description/purpose
     */
    private String description;

    // Validation happens in Controller or @Validated
    // Example: @NotBlank, @NotNull, @Positive, etc.
}

/**
 * Update[ENTITY_NAME]Request
 * Request body for PUT /api/v1/[entities]/{id}
 *
 * Example:
 * {
 *   "status": "PENDING"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Update[ENTITY_NAME]Request {

    /**
     * New status for state transition
     */
    private String status;

    /**
     * Optional: Reason for status change
     */
    private String reason;
}

/**
 * List[ENTITY_NAME]Request
 * Query parameters for GET /api/v1/[entities]
 *
 * Note: Spring automatically maps query params to this class if used
 * Or use @RequestParam in controller method signature
 *
 * Example:
 * GET /api/v1/payments?merchant=MER001&status=SUCCESS&page=0&size=20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class List[ENTITY_NAME]Request {

    /**
     * Filter by merchant UUID
     */
    private String merchant;

    /**
     * Filter by status
     */
    private String status;

    /**
     * Pagination: page number (0-indexed)
     */
    private Integer page = 0;

    /**
     * Pagination: items per page
     */
    private Integer size = 20;
}

// ==================== RESPONSE DTOs ====================

/**
 * [ENTITY_NAME]Response
 * Response body for all GET/POST endpoints
 *
 * Fields:
 * - id: UUID (external reference, safe to share)
 * - status: Current state (CREATED, PENDING, SUCCESS, etc.)
 * - amount: In rupees (converted from paise)
 * - expiresAt: Expiry time (ISO 8601 UTC)
 *
 * Hidden fields (not exposed):
 * - Long id: Database PK (internal only)
 * - Merchant object: Return merchantId only
 * - createdAt/updatedAt: Audit trail (not in API response)
 * - deletedAt: Soft delete marker (not exposed)
 *
 * Example:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440001",
 *   "status": "PENDING",
 *   "amount": 250.00,
 *   "currency": "INR",
 *   "expires_at": "2026-01-10T14:30:00Z"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class [ENTITY_NAME]Response {

    /**
     * [ENTITY_NAME] UUID (external reference)
     * Safe to share with customers, webhooks, etc.
     */
    @JsonProperty("id")
    private String uuid;

    /**
     * Current status (CREATED, PENDING, SUCCESS, etc.)
     */
    private String status;

    /**
     * Amount in rupees (converted from paise for API)
     * Backend stored as paise: 10050 (₹100.50)
     * API returns: 100.50 (BigDecimal)
     */
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217)
     */
    private String currency;

    /**
     * When payment QR code expires (ISO 8601 UTC)
     * Example: "2026-01-10T14:30:00Z"
     */
    @JsonProperty("expires_at")
    private Instant expiresAt;

    /**
     * When payment was created (optional, for audit)
     */
    @JsonProperty("created_at")
    private Instant createdAt;

    /**
     * Constructor from entity
     * Automatically converts paise → rupees, formats timestamps
     */
    public [ENTITY_NAME]Response([ENTITY_NAME] entity) {
        this.uuid = entity.get[ENTITY_NAME]Uuid();
        this.status = entity.getStatus().toString();
        this.amount = convertToRupees(entity.getAmountInPaise());
        this.currency = entity.getCurrency();
        this.expiresAt = entity.getExpiresAt();
        this.createdAt = entity.getCreatedAt();
    }

    /**
     * Convert paise (BIGINT) to rupees (BigDecimal)
     * Example: 10050 → 100.50
     */
    private BigDecimal convertToRupees(long paise) {
        return BigDecimal.valueOf(paise)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}

/**
 * [ENTITY_NAME]DetailResponse
 * Extended response with more details (optional, for specific endpoints)
 *
 * Use this for GET /api/v1/[entities]/{id} to return merchant, fees, etc.
 *
 * Example:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440001",
 *   "status": "SUCCESS",
 *   "amount": 250.00,
 *   "merchant": {
 *     "id": "MER001",
 *     "name": "ABC Store"
 *   },
 *   "platform_fee": 6.25,
 *   "merchant_net": 243.75,
 *   "expires_at": "2026-01-10T14:30:00Z"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class [ENTITY_NAME]DetailResponse extends [ENTITY_NAME]Response {

    /**
     * Merchant info (nested object)
     */
    private MerchantDTO merchant;

    /**
     * Platform fee taken (rupees)
     */
    @JsonProperty("platform_fee")
    private BigDecimal platformFee;

    /**
     * Merchant's net amount after fee (rupees)
     */
    @JsonProperty("merchant_net")
    private BigDecimal merchantNet;

    /**
     * Constructor from entity with merchant details
     */
    public [ENTITY_NAME]DetailResponse([ENTITY_NAME] entity) {
        super(entity);
        this.merchant = new MerchantDTO(
            entity.getMerchant().getMerchantUuid(),
            entity.getMerchant().getName()
        );
        this.platformFee = convertToRupees(entity.getPlatformFeeInPaise());
        this.merchantNet = convertToRupees(entity.getMerchantNetInPaise());
    }

    private BigDecimal convertToRupees(long paise) {
        return BigDecimal.valueOf(paise)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Nested merchant DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantDTO {
        private String id;
        private String name;
    }
}

// ==================== COMMON RESPONSE WRAPPER ====================

/**
 * PageResponse<T>
 * Wrapper for paginated responses
 *
 * Example:
 * {
 *   "content": [...],
 *   "total_elements": 42,
 *   "total_pages": 5,
 *   "current_page": 0,
 *   "page_size": 10
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * List of items in current page
     */
    private java.util.List<T> content;

    /**
     * Total number of items across all pages
     */
    @JsonProperty("total_elements")
    private long totalElements;

    /**
     * Total number of pages
     */
    @JsonProperty("total_pages")
    private int totalPages;

    /**
     * Current page number (0-indexed)
     */
    @JsonProperty("current_page")
    private int currentPage;

    /**
     * Items per page
     */
    @JsonProperty("page_size")
    private int pageSize;
}

/**
 * ErrorResponse
 * Standard error response for all endpoints
 *
 * Example:
 * {
 *   "error": "BAD_REQUEST",
 *   "message": "Amount must be > 0",
 *   "timestamp": "2026-01-10T10:15:30Z",
 *   "path": "/api/v1/payments"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Error code (machine-readable)
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * When error occurred (for logging/debugging)
     */
    private Instant timestamp;

    /**
     * API endpoint that caused the error
     */
    private String path;

    /**
     * HTTP status code
     */
    private int status;
}
