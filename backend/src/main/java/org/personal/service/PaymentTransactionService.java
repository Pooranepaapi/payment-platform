package org.personal.service;

import org.personal.entity.PaymentOrder;
import org.personal.entity.PaymentTransaction;
import org.personal.enums.TransactionStatus;
import org.personal.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentTransactionService
 * Manages payment transaction records (PSP interactions)
 * 
 * Responsibilities:
 * - Create transaction records for PSP interactions
 * - Update transaction status based on PSP responses
 * - Check idempotency (prevent duplicate processing)
 * - Track PSP approval codes and failure reasons
 */
@Service
@Transactional
public class PaymentTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentTransactionService.class);
    
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentTransactionService(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Create a new payment transaction (INITIATED state)
     * @param payment PaymentOrder entity
     * @param pspName PSP name (e.g., "AXIS", "HDFC")
     * @return Created PaymentTransaction
     */
    public PaymentTransaction createTransaction(PaymentOrder payment, String pspName) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentOrder(payment);
        transaction.setPspName(pspName);
        transaction.setStatus(TransactionStatus.INITIATED);
        transaction.setTransactionUuid(UUID.randomUUID().toString());
        transaction.setPspTransactionId(generatePspTransactionId(pspName));
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        logger.info("Transaction created: UUID={}, PSP={}", transaction.getTransactionUuid(), pspName);

        return saved;
    }

    /**
     * Update transaction status to PENDING (sent to PSP)
     * @param transactionId Transaction ID
     */
    public void markAsPending(Long transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setUpdatedAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        logger.info("Transaction marked as PENDING: {}", transaction.getTransactionUuid());
    }

    /**
     * Mark transaction as successful with PSP approval code
     * @param transactionId Transaction ID
     * @param pspApprovalCode PSP's approval code
     */
    public void markAsSuccess(Long transactionId, String pspApprovalCode) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setPspApprovalCode(pspApprovalCode);
        transaction.setUpdatedAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        logger.info("Transaction marked as SUCCESS: {}, approval_code={}", 
                   transaction.getTransactionUuid(), pspApprovalCode);
    }

    /**
     * Mark transaction as failed with failure reason
     * @param transactionId Transaction ID
     * @param failureReason Failure reason from PSP
     */
    public void markAsFailed(Long transactionId, String failureReason) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(failureReason);
        transaction.setUpdatedAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        logger.info("Transaction marked as FAILED: {}, reason={}", 
                   transaction.getTransactionUuid(), failureReason);
    }

    /**
     * Find transaction by PSP transaction ID (for idempotency)
     * Prevents duplicate processing if same PSP ID arrives twice
     * @param pspTransactionId PSP's transaction ID
     * @return Existing transaction or empty if new
     */
    public Optional<PaymentTransaction> findByPspTransactionId(String pspTransactionId) {
        return paymentTransactionRepository.findByPspTransactionId(pspTransactionId);
    }

    /**
     * Check if PSP transaction ID already processed (idempotency)
     * @param pspTransactionId PSP's transaction ID
     * @return true if already processed
     */
    public boolean isAlreadyProcessed(String pspTransactionId) {
        Optional<PaymentTransaction> existing = findByPspTransactionId(pspTransactionId);
        return existing.isPresent() && existing.get().getStatus() == TransactionStatus.SUCCESS;
    }

    /**
     * Get all transactions for a payment
     * @param payment PaymentOrder entity
     * @return List of transactions
     */
    public List<PaymentTransaction> getTransactionsByPayment(PaymentOrder payment) {
        return paymentTransactionRepository.findByPaymentOrder(payment);
    }

    /**
     * Get most recent (latest) transaction for a payment
     * @param payment PaymentOrder entity
     * @return Latest transaction
     */
    public Optional<PaymentTransaction> getLatestTransaction(PaymentOrder payment) {
        return paymentTransactionRepository.findLatestByPaymentOrder(payment);
    }

    /**
     * Get successful transaction for a payment (if exists)
     * @param payment PaymentOrder entity
     * @return Successful transaction
     */
    public Optional<PaymentTransaction> getSuccessfulTransaction(PaymentOrder payment) {
        return paymentTransactionRepository.findSuccessfulTransaction(payment);
    }

    /**
     * Generate unique PSP transaction ID
     * Format: {PSP_NAME}_{TIMESTAMP}_{UUID}
     * @param pspName PSP name
     * @return Unique PSP transaction ID
     */
    private String generatePspTransactionId(String pspName) {
        return pspName + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID();
    }
}
