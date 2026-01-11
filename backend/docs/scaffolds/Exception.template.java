package org.personal.exception;

/**
 * Custom Exceptions for [ENTITY_NAME]
 *
 * Exception Hierarchy:
 *
 * RuntimeException (Unchecked, causes transaction rollback)
 * ├── ApplicationException (Custom base)
 * │   ├── BadRequestException (HTTP 400)
 * │   ├── ResourceNotFoundException (HTTP 404)
 * │   ├── BusinessException (HTTP 422)
 * │   ├── InvalidStateException (HTTP 409)
 * │   └── DuplicateResourceException (HTTP 409)
 * └── Spring Built-ins
 *     ├── DataIntegrityViolationException (HTTP 409)
 *     └── OptimisticLockingFailureException (HTTP 409)
 *
 * Usage:
 * - Throw these from Service layer
 * - Catch and convert to HTTP responses in @ExceptionHandler (centralized)
 * - Never catch and ignore exceptions silently
 *
 * Rules:
 * 1. Always include meaningful error messages
 * 2. Always include cause (root exception) for debugging
 * 3. Use @Transactional: Unchecked exceptions cause rollback
 * 4. Use specific exceptions (not generic Exception)
 */

// ==================== Base Exception ====================

/**
 * ApplicationException
 * Base class for all custom exceptions
 *
 * Extends RuntimeException so @Transactional automatically rolls back
 * (Checked exceptions don't rollback by default)
 */
public class ApplicationException extends RuntimeException {

    /**
     * HTTP status code
     * Subclasses override to set appropriate status
     */
    private final int httpStatus;

    /**
     * Error code (machine-readable)
     * Examples: "MERCHANT_NOT_FOUND", "INVALID_STATE", etc.
     */
    private final String errorCode;

    public ApplicationException(int httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public ApplicationException(int httpStatus, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

// ==================== Specific Exceptions ====================

/**
 * BadRequestException (HTTP 400)
 *
 * Use when:
 * - Input validation fails (missing fields, invalid format, etc.)
 * - Precondition not met
 *
 * Examples:
 * - Amount must be > 0
 * - Invalid UUID format
 * - Merchant ID is empty
 */
public class BadRequestException extends ApplicationException {

    public BadRequestException(String message) {
        super(400, "BAD_REQUEST", message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(400, "BAD_REQUEST", message, cause);
    }
}

/**
 * ResourceNotFoundException (HTTP 404)
 *
 * Use when:
 * - Entity doesn't exist
 * - Merchant not found
 * - Payment not found
 *
 * Examples:
 * - "Merchant not found: MER001"
 * - "[ENTITY_NAME] not found: uuid=550e8400-..."
 */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(404, "NOT_FOUND", message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(404, "NOT_FOUND", message, cause);
    }
}

/**
 * InvalidStateException (HTTP 409 Conflict)
 *
 * Use when:
 * - State transition is invalid
 * - Operation not allowed in current state
 * - State machine violation
 *
 * Examples:
 * - "Cannot transition from CREATED to FAILED"
 * - "Payment must be PENDING to capture"
 * - "Cannot refund PENDING payment"
 */
public class InvalidStateException extends ApplicationException {

    public InvalidStateException(String message) {
        super(409, "INVALID_STATE", message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(409, "INVALID_STATE", message, cause);
    }
}

/**
 * DuplicateResourceException (HTTP 409 Conflict)
 *
 * Use when:
 * - Unique constraint violation
 * - Resource with same UUID already exists
 *
 * Examples:
 * - "Merchant with VPA 'store@axis' already exists"
 * - "[ENTITY_NAME] UUID already exists"
 */
public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException(String message) {
        super(409, "DUPLICATE_RESOURCE", message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(409, "DUPLICATE_RESOURCE", message, cause);
    }
}

/**
 * BusinessException (HTTP 422 Unprocessable Entity)
 *
 * Use when:
 * - Business rule violated (not validation, but business logic)
 * - Operation not permitted due to entity state
 *
 * Examples:
 * - "Merchant is not active"
 * - "Cannot refund more than original amount"
 * - "Payment has already been processed"
 */
public class BusinessException extends ApplicationException {

    public BusinessException(String message) {
        super(422, "BUSINESS_ERROR", message);
    }

    public BusinessException(String message, Throwable cause) {
        super(422, "BUSINESS_ERROR", message, cause);
    }
}

/**
 * ExternalServiceException (HTTP 502 Bad Gateway)
 *
 * Use when:
 * - Third-party API call fails (PSP, bank, etc.)
 * - Network timeout
 * - Service unavailable
 *
 * Examples:
 * - "PSP service timeout"
 * - "Bank returned error code: 5001"
 */
public class ExternalServiceException extends ApplicationException {

    public ExternalServiceException(String message) {
        super(502, "EXTERNAL_SERVICE_ERROR", message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(502, "EXTERNAL_SERVICE_ERROR", message, cause);
    }
}

/**
 * InternalServerException (HTTP 500)
 *
 * Use when:
 * - Unexpected error (catch-all for unhandled exceptions)
 * - System configuration error
 * - Database connection issue
 *
 * Examples:
 * - "Database connection lost"
 * - "Unknown error processing payment"
 */
public class InternalServerException extends ApplicationException {

    public InternalServerException(String message) {
        super(500, "INTERNAL_SERVER_ERROR", message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(500, "INTERNAL_SERVER_ERROR", message, cause);
    }
}

// ==================== Global Exception Handler ====================

/**
 * GlobalExceptionHandler
 *
 * Location: org.personal.exception.GlobalExceptionHandler
 *
 * Handles all exceptions thrown by @Service classes
 * Converts exceptions to HTTP responses with appropriate status codes
 *
 * Usage:
 * @RestControllerAdvice
 * public class GlobalExceptionHandler {
 *
 *     @ExceptionHandler(ResourceNotFoundException.class)
 *     public ResponseEntity<ErrorResponse> handleNotFound(
 *         ResourceNotFoundException e,
 *         HttpServletRequest request) {
 *
 *         ErrorResponse error = new ErrorResponse(
 *             e.getErrorCode(),
 *             e.getMessage(),
 *             Instant.now(),
 *             request.getRequestURI(),
 *             404
 *         );
 *
 *         return ResponseEntity
 *             .status(HttpStatus.NOT_FOUND)
 *             .body(error);
 *     }
 *
 *     @ExceptionHandler(ApplicationException.class)
 *     public ResponseEntity<ErrorResponse> handleApplicationException(
 *         ApplicationException e,
 *         HttpServletRequest request) {
 *
 *         ErrorResponse error = new ErrorResponse(
 *             e.getErrorCode(),
 *             e.getMessage(),
 *             Instant.now(),
 *             request.getRequestURI(),
 *             e.getHttpStatus()
 *         );
 *
 *         return ResponseEntity
 *             .status(e.getHttpStatus())
 *             .body(error);
 *     }
 *
 *     @ExceptionHandler(Exception.class)
 *     public ResponseEntity<ErrorResponse> handleGenericException(
 *         Exception e,
 *         HttpServletRequest request) {
 *
 *         log.error("Unexpected error", e);
 *         ErrorResponse error = new ErrorResponse(
 *             "INTERNAL_SERVER_ERROR",
 *             "An unexpected error occurred",
 *             Instant.now(),
 *             request.getRequestURI(),
 *             500
 *         );
 *
 *         return ResponseEntity
 *             .status(HttpStatus.INTERNAL_SERVER_ERROR)
 *             .body(error);
 *     }
 * }
 */
