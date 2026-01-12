package org.personal.simulator.enums;

/**
 * Payment types supported by the simulator.
 * Matches backend PaymentType enum.
 */
public enum PaymentType {
    // UPI types
    RBLUPI,
    HDFCUPI,
    KOTAKUPI,

    // Card types (Phase 2)
    HDFCCC,
    RBLCC,

    // NetBanking types (Phase 2)
    HDFCNB,
    ABORBNB
}
