package org.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.personal.enums.PaymentEventType;

import java.time.LocalDateTime;

/**
 * PaymentEvent Entity (Immutable Audit Trail)
 *
 * Append-only log of all state changes in a payment lifecycle.
 * Implements event sourcing pattern (lite version).
 *
 * Design Principle:
 * - Immutable: Never updated or deleted
 * - Append-only: New records added as events occur
 * - Complete history: Can reconstruct payment state from events
 * - Non-repudiation: Proof of when things happened
 *
 * Lifecycle Example:
 * 1. Event: CREATED (payment created, status = CREATED)
 * 2. Event: QR_GENERATED (QR generated, status = QR_GENERATED)
 * 3. Event: PENDING (customer approved, status = PENDING)
 * 4. Event: SUCCESS (PSP confirmed, status = SUCCESS)
 *
 * With Refund:
 * 5. Event: REFUND_INITIATED (refund requested)
 *
 * Use Cases:
 * - Audit trail: See what happened to this payment
 * - Debugging: Understand payment flow
 * - Compliance: Proof of state changes
 * - Webhooks (Phase-2): Emit events to external systems
 * - Analytics (Phase-3): Analyze payment patterns
 * - Cold storage (Phase-3): Move old events to archive
 *
 * Relationship:
 * - Many-to-One: Many events belong to one PaymentOrder
 *
 * Query Patterns:
 * - Get all events for payment (audit trail)
 * - Get events by type (count SUCCESS, count FAILED, etc.)
 * - Get latest event (determine current state)
 * - Get events in time range (for reporting)
 */
@Entity
@Table(
    name = "payment_event",
    indexes = {
        @Index(name = "idx_event_payment_time", columnList = "payment_uuid,created_at"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_created", columnList = "created_at DESC")
    }
)
@Getter
@Setter
public class PaymentEvent {

    // ==================== Primary Key ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Payment Reference ====================

    /**
     * Which payment does this event belong to?
     * - Many-to-One relationship
     * - Foreign key: payment_order_id
     * - Never null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    /**
     * Payment UUID (denormalized for faster queries)
     * - Copied from PaymentOrder.paymentUuid
     * - Allows querying events without joining to PaymentOrder
     * - Index: idx_event_payment_time(payment_uuid, created_at)
     * - For common query: "Give me all events for this payment"
     */
    @Column(name = "payment_uuid", nullable = false, length = 36)
    private String paymentUuid;

    // ==================== Event Type ====================

    /**
     * What happened?
     * Examples:
     * - CREATED: Payment order created
     * - QR_GENERATED: QR code generated
     * - PENDING: Customer approved payment
     * - SUCCESS: Payment succeeded
     * - FAILED: Payment failed
     * - EXPIRED: QR expired
     * - CANCELLED: Payment cancelled
     * - REFUND_INITIATED: Refund requested
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private PaymentEventType eventType;

    // ==================== Metadata (Optional) ====================

    /**
     * Additional context about this event (JSON format)
     * - Optional, flexible structure
     * - Examples:
     *   {
     *     "pspTransactionId": "AXIS_TXN_123",
     *     "pspApprovalCode": "ABC123XYZ",
     *     "psp": "AXIS",
     *     "approvalTime": "2026-01-10T10:05:30Z",
     *     "source": "UPI_APP"
     *   }
     *
     *   Or for failure:
     *   {
     *     "failureReason": "Insufficient funds",
     *     "errorCode": "INSUFFICIENT_BALANCE"
     *   }
     *
     *   Or for refund:
     *   {
     *     "refundUuid": "ref_abc123",
     *     "refundAmount": 5000,
     *     "reason": "Customer request"
     *   }
     *
     * Non-PII: Never include customer details, card numbers, etc.
     * JSON column: Can query with JSON operators (Phase-2+)
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    // ==================== Timestamps ====================

    /**
     * When did this event occur?
     * - Immutable (updatable = false)
     * - Events have ordering: ORDER BY created_at ASC
     * - Precision: milliseconds (LocalDateTime)
     * - UTC/system timezone
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== Lifecycle Callbacks ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Check if event is a terminal state event
     * (no more state changes will occur after this)
     */
    public boolean isTerminalEvent() {
        return eventType == PaymentEventType.SUCCESS ||
               eventType == PaymentEventType.FAILED ||
               eventType == PaymentEventType.EXPIRED ||
               eventType == PaymentEventType.CANCELLED;
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "id=" + id +
                ", paymentUuid='" + paymentUuid + '\'' +
                ", eventType=" + eventType +
                ", createdAt=" + createdAt +
                '}';
    }
}
