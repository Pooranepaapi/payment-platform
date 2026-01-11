package org.personal.enums;

/**
 * Refund Status for RefundOrder entity
 *
 * Tracks the lifecycle of refund requests.
 * One payment can have multiple refunds (partial refunds supported).
 *
 * State transition:
 * INITIATED → PENDING → SUCCESS/FAILED
 */
public enum RefundStatus {
    INITIATED,  // Refund requested (waiting to send to PSP)
    PENDING,    // Refund sent to PSP (waiting for response)
    SUCCESS,    // Refund completed successfully
    FAILED      // Refund failed or declined by PSP
}
