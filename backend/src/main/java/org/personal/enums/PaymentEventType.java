package org.personal.enums;

/**
 * Payment Event Types for audit trail
 *
 * Immutable log of all state changes in a payment lifecycle.
 * Used by PaymentEvent entity for complete audit trail.
 *
 * Events follow payment state transitions:
 * CREATED → QR_GENERATED → PENDING → SUCCESS/FAILED/EXPIRED/CANCELLED
 * Plus: REFUND_INITIATED for refunds
 */
public enum PaymentEventType {
    // Core lifecycle events
    CREATED,           // PaymentOrder created (status = CREATED)
    QR_GENERATED,      // QR code generated (status = QR_GENERATED)
    PENDING,           // Awaiting customer approval (status = PENDING)
    SUCCESS,           // Payment successful (status = SUCCESS)
    FAILED,            // Payment failed/declined (status = FAILED)
    EXPIRED,           // QR code expired (status = EXPIRED)
    CANCELLED,         // Payment cancelled (status = CANCELLED)

    // Refund events
    REFUND_INITIATED   // Refund requested (status unchanged, creates RefundOrder)
}
