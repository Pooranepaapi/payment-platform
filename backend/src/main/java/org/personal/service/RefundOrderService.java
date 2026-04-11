package org.personal.service;

import org.personal.entity.Payment;
import org.personal.entity.RefundOrder;
import org.personal.enums.RefundStatus;
import org.personal.repository.RefundOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RefundOrderService
 * Manages refund lifecycle for payments
 * 
 * Responsibilities:
 * - Create refund orders (partial or full)
 * - Track refund status
 * - Validate refund amounts (no over-refund)
 * - Calculate total refunded amounts
 */
@Service
@Transactional
public class RefundOrderService {

    private static final Logger logger = LoggerFactory.getLogger(RefundOrderService.class);
    
    private final RefundOrderRepository refundOrderRepository;

    public RefundOrderService(RefundOrderRepository refundOrderRepository) {
        this.refundOrderRepository = refundOrderRepository;
    }

    /**
     * Create a new refund order (INITIATED state)
     * Validates refund amount is not over-refunding
     * @param payment Payment entity
     * @param refundAmountInPaise Amount to refund in paise
     * @param reason Refund reason (optional)
     * @return Created RefundOrder
     */
    public RefundOrder createRefund(Payment payment, Long refundAmountInPaise, String reason) {
        // Validate amount
        if (refundAmountInPaise == null || refundAmountInPaise <= 0) {
            throw new IllegalArgumentException("Refund amount must be > 0");
        }

        // Check not over-refunding
        Long totalRefunded = refundOrderRepository.sumSuccessfulRefundAmount(payment);
        if (totalRefunded == null) {
            totalRefunded = 0L;
        }

        long maxRefund = payment.getAmountInPaise() - totalRefunded;
        if (refundAmountInPaise > maxRefund) {
            throw new IllegalArgumentException("Refund amount exceeds maximum allowed: " + maxRefund);
        }

        RefundOrder refund = new RefundOrder();
        refund.setPayment(payment);
        refund.setRefundAmountInPaise(refundAmountInPaise);
        refund.setReason(reason);
        refund.setStatus(RefundStatus.INITIATED);
        refund.setRefundUuid(UUID.randomUUID().toString());
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());

        RefundOrder saved = refundOrderRepository.save(refund);
        logger.info("Refund created: UUID={}, amount_paise={}", refund.getRefundUuid(), refundAmountInPaise);

        return saved;
    }

    /**
     * Mark refund as pending (sent to PSP)
     * @param refundId Refund ID
     */
    public void markAsPending(Long refundId) {
        RefundOrder refund = refundOrderRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        refund.setStatus(RefundStatus.PENDING);
        refund.setUpdatedAt(LocalDateTime.now());
        refundOrderRepository.save(refund);

        logger.info("Refund marked as PENDING: {}", refund.getRefundUuid());
    }

    /**
     * Mark refund as successful
     * @param refundId Refund ID
     */
    public void markAsSuccess(Long refundId) {
        RefundOrder refund = refundOrderRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        refund.setStatus(RefundStatus.SUCCESS);
        refund.setUpdatedAt(LocalDateTime.now());
        refundOrderRepository.save(refund);

        logger.info("Refund marked as SUCCESS: {}", refund.getRefundUuid());
    }

    /**
     * Mark refund as failed
     * @param refundId Refund ID
     */
    public void markAsFailed(Long refundId) {
        RefundOrder refund = refundOrderRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        refund.setStatus(RefundStatus.FAILED);
        refund.setUpdatedAt(LocalDateTime.now());
        refundOrderRepository.save(refund);

        logger.info("Refund marked as FAILED: {}", refund.getRefundUuid());
    }

    /**
     * Get all refunds for a payment
     * @param payment Payment entity
     * @return List of refunds
     */
    public List<RefundOrder> getRefundsByPayment(Payment payment) {
        return refundOrderRepository.findByPayment(payment);
    }

    /**
     * Get successful refunds for a payment
     * Used to calculate total refunded amount
     * @param payment Payment entity
     * @return List of successful refunds
     */
    public List<RefundOrder> getSuccessfulRefunds(Payment payment) {
        return refundOrderRepository.findSuccessfulRefunds(payment);
    }

    /**
     * Calculate total refunded amount for a payment
     * @param payment Payment entity
     * @return Total refunded in paise
     */
    public Long getTotalRefundedAmount(Payment payment) {
        Long total = refundOrderRepository.sumSuccessfulRefundAmount(payment);
        return total != null ? total : 0L;
    }

    /**
     * Find refund by UUID
     * @param refundUuid Refund UUID
     * @return RefundOrder if found
     */
    public Optional<RefundOrder> findByRefundUuid(String refundUuid) {
        return refundOrderRepository.findByRefundUuid(refundUuid);
    }

    /**
     * Check if refund amount is valid (not over-refunding)
     * @param payment Payment entity
     * @param refundAmountInPaise Amount to refund
     * @return true if valid
     */
    public boolean isValidRefundAmount(Payment payment, Long refundAmountInPaise) {
        Long totalRefunded = getTotalRefundedAmount(payment);
        long maxRefund = payment.getAmountInPaise() - totalRefunded;
        return refundAmountInPaise > 0 && refundAmountInPaise <= maxRefund;
    }
}
