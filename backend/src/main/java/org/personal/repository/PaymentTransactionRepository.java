package org.personal.repository;

import org.personal.entity.PaymentTransaction;
import org.personal.entity.PaymentOrder;
import org.personal.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentTransaction entity
 * Handles database operations for payment transactions (PSP interactions)
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find transaction by UUID (external reference for audit trail, webhooks)
     * @param transactionUuid Unique identifier
     * @return PaymentTransaction if found
     */
    Optional<PaymentTransaction> findByTransactionUuid(String transactionUuid);

    /**
     * Find transaction by PSP transaction ID (UNIQUE, idempotency key)
     * Used to prevent duplicate processing when retry happens
     * @param pspTransactionId PSP's unique transaction identifier
     * @return PaymentTransaction if found
     */
    Optional<PaymentTransaction> findByPspTransactionId(String pspTransactionId);

    /**
     * Find all transactions for a payment order
     * @param paymentOrder PaymentOrder entity
     * @return List of transactions for this payment
     */
    List<PaymentTransaction> findByPaymentOrder(PaymentOrder paymentOrder);

    /**
     * Find all transactions for a payment with given status
     * @param paymentOrder PaymentOrder entity
     * @param status TransactionStatus
     * @return List of transactions
     */
    List<PaymentTransaction> findByPaymentOrderAndStatus(PaymentOrder paymentOrder, TransactionStatus status);

    /**
     * Find the most recent transaction for a payment
     * @param paymentOrder PaymentOrder entity
     * @return Latest PaymentTransaction
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE t.paymentOrder = :paymentOrder " +
           "ORDER BY t.createdAt DESC LIMIT 1")
    Optional<PaymentTransaction> findLatestByPaymentOrder(@Param("paymentOrder") PaymentOrder paymentOrder);

    /**
     * Find all pending transactions (waiting for PSP response)
     * @return List of pending transactions
     */
    List<PaymentTransaction> findByStatus(TransactionStatus status);

    /**
     * Find successful transactions for a payment
     * @param paymentOrder PaymentOrder entity
     * @return Successful transaction if exists
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE t.paymentOrder = :paymentOrder " +
           "AND t.status = org.personal.enums.TransactionStatus.SUCCESS")
    Optional<PaymentTransaction> findSuccessfulTransaction(@Param("paymentOrder") PaymentOrder paymentOrder);

    /**
     * Find transactions for a specific PSP
     * @param pspName PSP name (e.g., "AXIS", "HDFC")
     * @return List of transactions for that PSP
     */
    List<PaymentTransaction> findByPspName(String pspName);

    /**
     * Find failed transactions created within a time range
     * Used for failure analysis and retry strategies
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return List of failed transactions
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = org.personal.enums.TransactionStatus.FAILED " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findFailedTransactionsInRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Check if transaction UUID exists
     * @param transactionUuid UUID to check
     * @return true if exists
     */
    boolean existsByTransactionUuid(String transactionUuid);

    /**
     * Check if PSP transaction ID is already used (for idempotency)
     * @param pspTransactionId PSP transaction ID to check
     * @return true if exists
     */
    boolean existsByPspTransactionId(String pspTransactionId);

    /**
     * Count transactions by status for a payment
     * @param paymentOrder PaymentOrder entity
     * @param status TransactionStatus
     * @return Count of transactions
     */
    long countByPaymentOrderAndStatus(PaymentOrder paymentOrder, TransactionStatus status);
}
