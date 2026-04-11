package org.personal.enums;

/**
 * Payment status.
 * CREATED → QR_GENERATED → PENDING → SUCCESS / FAILED / EXPIRED / CANCELLED
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
