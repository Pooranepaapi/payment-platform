package org.personal.controller;

import org.personal.entity.Payment;
import org.personal.entity.Transaction;
import org.personal.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Transaction Controller
 * Provides read-only access to individual transaction details.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Get transaction by ID or UUID.
     * GET /api/transactions/{transactionId}
     *
     * Accepts either a numeric ID or a UUID string.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<Map<String, Object>> getTransaction(@PathVariable String transactionId) {

        logger.info("Fetching transaction: {}", transactionId);

        Transaction txn;
        try {
            Long id = Long.parseLong(transactionId);
            txn = transactionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        } catch (NumberFormatException e) {
            txn = transactionRepository.findByTransactionUuid(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        }

        Payment payment = txn.getPayment();

        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", txn.getId());
        response.put("transactionUuid", txn.getTransactionUuid());
        response.put("paymentId", payment != null ? payment.getId() : null);
        response.put("paymentUuid", payment != null ? payment.getPaymentUuid() : null);
        response.put("status", txn.getStatus());
        response.put("paymentMethod", "UPI_QR");
        response.put("amount", payment != null ? payment.getAmountInRupees() : null);
        response.put("currency", payment != null ? payment.getCurrency() : null);
        response.put("pspReferenceId", txn.getPspTransactionId());
        response.put("authCode", txn.getPspApprovalCode());
        response.put("bankReferenceId", txn.getBankReferenceId());
        response.put("failureReason", txn.getFailureReason());
        response.put("createdAt", txn.getCreatedAt());
        response.put("success", txn.isSuccessful());
        response.put("message", txn.isSuccessful() ? "Transaction successful" : "Transaction " + txn.getStatus().name().toLowerCase());

        return ResponseEntity.ok(response);
    }
}
