package org.personal.repository;

import org.personal.entity.RefundOrder;
import org.personal.entity.PaymentOrder;
import org.personal.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefundOrder entity
 * Handles database operations for refund orders (many refunds per payment)
 */
@Repository
public interface RefundOrderRepository extends JpaRepository<RefundOrder, Long> {

    /**
     * Find refund by UUID (external reference for customer communication, webhooks)
     * @param refundUuid Unique identifier
     * @return RefundOrder if found
     */
    Optional<RefundOrder> findByRefundUuid(String refundUuid);

    /**
     * Find all refunds for a payment
     * Supports: full refund (1 refund) or partial refunds (multiple refunds)
     * @param paymentOrder PaymentOrder entity
     * @return List of refunds for this payment
     */
    List<RefundOrder> findByPaymentOrder(PaymentOrder paymentOrder);

    /**
     * Find all refunds for a payment with given status
     * Example: Find all PENDING refunds waiting for PSP response
     * @param paymentOrder PaymentOrder entity
     * @param status RefundStatus
     * @return List of refunds with that status
     */
    List<RefundOrder> findByPaymentOrderAndStatus(PaymentOrder paymentOrder, RefundStatus status);

    /**
     * Find all successful refunds for a payment
     * Used to calculate total refunded amount
     * @param paymentOrder PaymentOrder entity
     * @return List of successful refunds
     */
    @Query("SELECT r FROM RefundOrder r WHERE r.paymentOrder = :paymentOrder AND r.status = org.personal.enums.RefundStatus.SUCCESS")
    List<RefundOrder> findSuccessfulRefunds(@Param("paymentOrder") PaymentOrder paymentOrder);

    /**
     * Calculate total refunded amount for a payment
     * Used for reconciliation: verify no over-refund
     * @param paymentOrder PaymentOrder entity
     * @return Sum of refund amounts (in paise) or 0 if no refunds
     */
    @Query("SELECT SUM(r.refundAmountInPaise) FROM RefundOrder r WHERE r.paymentOrder = :paymentOrder AND r.status = org.personal.enums.RefundStatus.SUCCESS")
    Long sumSuccessfulRefundAmount(@Param("paymentOrder") PaymentOrder paymentOrder);

    /**
     * Find all pending refunds (awaiting PSP response)
     * @return List of pending refunds
     */
    List<RefundOrder> findByStatus(RefundStatus status);

    /**
     * Find all refunds created within a time range
     * Used for daily reports, analytics
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return List of refunds in range
     */
    @Query("SELECT r FROM RefundOrder r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<RefundOrder> findByCreatedAtRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find the most recent refund for a payment
     * @param paymentOrder PaymentOrder entity
     * @return Latest RefundOrder
     */
    @Query("SELECT r FROM RefundOrder r WHERE r.paymentOrder = :paymentOrder ORDER BY r.createdAt DESC LIMIT 1")
    Optional<RefundOrder> findLatestByPaymentOrder(@Param("paymentOrder") PaymentOrder paymentOrder);

    /**
     * Check if refund UUID exists
     * @param refundUuid UUID to check
     * @return true if exists
     */
    boolean existsByRefundUuid(String refundUuid);

    /**
     * Count refunds by status for a payment
     * @param paymentOrder PaymentOrder entity
     * @param status RefundStatus
     * @return Count of refunds
     */
    long countByPaymentOrderAndStatus(PaymentOrder paymentOrder, RefundStatus status);
}
