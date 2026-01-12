package org.personal.simulator.service;

import org.personal.simulator.enums.PaymentMethod;
import org.personal.simulator.enums.PaymentType;

/**
 * Top-level interface for all bank simulators.
 * Supports UPI, Card, and NetBanking payment methods.
 */
public interface BankSimulator {

    /**
     * Returns the PaymentType this simulator handles (e.g., RBLUPI, HDFCCC).
     */
    PaymentType getPaymentType();

    /**
     * Returns the PaymentMethod category (UPI, CARD, NETBANKING).
     */
    PaymentMethod getPaymentMethod();

    /**
     * Validates if this simulator supports the given payment type.
     */
    boolean supports(PaymentType paymentType);
}
