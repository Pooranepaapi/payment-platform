package org.personal.enums;

/**
 * Payment Status for both UPI (Payment) and QR (PaymentOrder) systems
 *
 * UPI System (Payment entity):
 * CREATED → PENDING → SUCCESS/FAILED/CANCELLED
 *
 * QR System (PaymentOrder entity):
 * CREATED → QR_GENERATED → PENDING → SUCCESS/FAILED/EXPIRED
 */
public enum PaymentStatus {
    // Common states
    CREATED,          // Payment order created
    PENDING,          // Awaiting customer action / PSP response
    SUCCESS,          // Payment successful
    FAILED,           // Payment failed / declined
    EXPIRED,          // QR code expired (Phase-1 QR only)
    CANCELLED,        // Payment cancelled (Phase-1 QR only)

    // QR-specific states
    QR_GENERATED,     // QR code generated (Phase-1 QR only)

    // UPI-specific states (legacy)
    REFUNDED,         // Payment refunded (legacy UPI)
    REFUNDED_PARTIALLY // Partial refund (legacy UPI)
}
