package org.personal.repository;

import org.personal.entity.PaymentOrder;
import org.personal.entity.Merchant;
import org.personal.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentOrder entity
 * Handles database operations for payment orders (QR payment system)
 */
@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    /**
     * Find PaymentOrder by UUID (external reference for APIs, webhooks)
     * @param paymentUuid Unique identifier
     * @return PaymentOrder if found
     */
    Optional<PaymentOrder> findByPaymentUuid(String paymentUuid);

    /**
     * Find all payments for a merchant with given status
     * @param merchant Merchant entity
     * @param status PaymentStatus
     * @return List of payment orders
     */
    List<PaymentOrder> findByMerchantAndStatus(Merchant merchant, PaymentStatus status);

    /**
     * Find all payments for a merchant
     * @param merchant Merchant entity
     * @return List of payment orders
     */
    List<PaymentOrder> findByMerchant(Merchant merchant);

    /**
     * Find all payments with given status
     * @param status PaymentStatus
     * @return List of payment orders
     */
    List<PaymentOrder> findByStatus(PaymentStatus status);

    /**
     * Find expired payments (past expiresAt, not in terminal state)
     * Used for periodic checks or reports
     * @param now Current timestamp
     * @return List of expired payments
     */
    @Query("SELECT p FROM PaymentOrder p WHERE p.expiresAt < :now AND " +
           "p.status NOT IN (org.personal.enums.PaymentStatus.SUCCESS, " +
           "org.personal.enums.PaymentStatus.FAILED, " +
           "org.personal.enums.PaymentStatus.EXPIRED, " +
           "org.personal.enums.PaymentStatus.CANCELLED)")
    List<PaymentOrder> findExpiredPayments(@Param("now") LocalDateTime now);

    /**
     * Find payments created within a time range
     * @param merchant Merchant entity
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return List of payment orders
     */
    @Query("SELECT p FROM PaymentOrder p WHERE p.merchant = :merchant AND " +
           "p.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentOrder> findByMerchantAndDateRange(
        @Param("merchant") Merchant merchant,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Check if payment UUID exists
     * @param paymentUuid UUID to check
     * @return true if exists
     */
    boolean existsByPaymentUuid(String paymentUuid);


    /**
     * Count payments by status for a merchant
     * @param merchant Merchant entity
     * @param status PaymentStatus
     * @return Count of payments
     */
    long countByMerchantAndStatus(Merchant merchant, PaymentStatus status);
}
