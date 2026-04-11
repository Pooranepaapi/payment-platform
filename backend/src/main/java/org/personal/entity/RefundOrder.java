package org.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.personal.enums.RefundStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefundOrder Entity
 *
 * Represents a refund request for a payment.
 * Supports partial and multiple refunds per payment.
 *
 * Design Principle:
 * - One payment can have many refunds
 * - Each refund has independent lifecycle
 * - Refund amounts can be partial (e.g., ₹50 of ₹100 payment)
 * - Each refund tracked separately for audit & reconciliation
 *
 * Scenarios:
 * 1. Full refund: One refund for entire payment amount
 * 2. Partial refund: Multiple refunds, sum < payment amount
 * 3. Split refund: Customer dispute → vendor issues multiple refunds
 *
 * Relationship:
 * - Many-to-One: Many RefundOrder belong to one PaymentOrder
 *
 * State Machine (Valid Transitions):
 * INITIATED
 *   └─→ PENDING (refund sent to PSP)
 *
 * PENDING
 *   └─→ SUCCESS (PSP refunded)
 *   └─→ FAILED (PSP declined refund)
 *
 * SUCCESS / FAILED
 *   └─→ (TERMINAL - no further changes)
 *
 * Example Refund Lifecycle:
 * 1. Customer requests refund
 * 2. RefundOrder created: status = INITIATED
 * 3. Refund sent to PSP
 * 4. RefundOrder updated: status = PENDING
 * 5. PSP response received
 * 6. RefundOrder updated: status = SUCCESS or FAILED
 */
@Entity
@Table(
    name = "refund_order",
    indexes = {
        @Index(name = "idx_refund_uuid", columnList = "refund_uuid", unique = true),
        @Index(name = "idx_refund_payment", columnList = "payment_id"),
        @Index(name = "idx_refund_status", columnList = "status"),
        @Index(name = "idx_refund_created", columnList = "created_at DESC")
    }
)
@Getter
@Setter
public class RefundOrder {

    // ==================== Primary Key ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== External References ====================

    /**
     * Refund UUID for external reference and tracking
     * - Unique globally
     * - Used in webhooks and customer communication
     * - For tracing specific refund request
     */
    @Column(name = "refund_uuid", nullable = false, unique = true, length = 36)
    private String refundUuid = UUID.randomUUID().toString();

    // ==================== Payment Reference ====================

    /**
     * Which payment is being refunded?
     * - Many-to-One relationship
     * - Foreign key: payment_order_id
     * - One payment can have multiple refunds
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // ==================== Refund Status ====================

    /**
     * Current refund status
     * Tracks the lifecycle of refund request
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status = RefundStatus.INITIATED;

    // ==================== Refund Amount ====================

    /**
     * Amount being refunded (in paise)
     * - ₹50.25 = 5025 paise
     * - BIGINT for precision (no floating-point errors)
     * - Must be > 0
     * - Can be less than payment amount (partial refund)
     * - Constraint: refundAmountInPaise <= paymentOrder.amountInPaise
     *
     * Validation Rules:
     * - Must be positive
     * - Cannot exceed original payment amount
     * - Sum of all refunds for payment ≤ original amount
     */
    @Column(name = "refund_amount_in_paise", nullable = false)
    private Long refundAmountInPaise;

    // ==================== Refund Reason ====================

    /**
     * Why is this refund being issued? (optional)
     * Examples:
     * - "Customer request"
     * - "Duplicate payment"
     * - "Product not delivered"
     * - "Merchant discount"
     * - "Quality issue"
     */
    @Column(name = "reason", length = 255)
    private String reason;

    // ==================== Timestamps ====================

    /**
     * When refund was requested
     * - Immutable (updatable = false)
     * - Set when RefundOrder created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last modified timestamp
     * - Updated when status changes
     * - (INITIATED → PENDING → SUCCESS/FAILED)
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== Lifecycle Callbacks ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (refundUuid == null) {
            refundUuid = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Helper Methods ====================

    /**
     * Check if refund is in terminal state (no further changes possible)
     */
    public boolean isTerminal() {
        return status == RefundStatus.SUCCESS || status == RefundStatus.FAILED;
    }

    /**
     * Check if refund succeeded
     */
    public boolean isSuccessful() {
        return status == RefundStatus.SUCCESS;
    }

    /**
     * Check if refund is still pending (not terminal)
     */
    public boolean isPending() {
        return status == RefundStatus.INITIATED || status == RefundStatus.PENDING;
    }

    /**
     * Convert amount from paise to rupees (for display)
     */
    public java.math.BigDecimal getAmountInRupees() {
        return java.math.BigDecimal.valueOf(refundAmountInPaise)
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "RefundOrder{" +
                "id=" + id +
                ", refundUuid='" + refundUuid + '\'' +
                ", status=" + status +
                ", refundAmountInPaise=" + refundAmountInPaise +
                ", createdAt=" + createdAt +
                '}';
    }
}
