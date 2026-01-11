package org.personal.controller;

import org.personal.dto.TransactionResponse;
import org.personal.dto.UpiCallbackRequest;
import org.personal.entity.Transaction;
import org.personal.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final PaymentService paymentService;

    public TransactionController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Get transaction by ID.
     * GET /api/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        Transaction transaction = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    /**
     * UPI callback endpoint (simulates PSP callback).
     * POST /api/transactions/upi/callback
     */
    @PostMapping("/upi/callback")
    public ResponseEntity<TransactionResponse> upiCallback(@RequestBody UpiCallbackRequest request) {
        Transaction transaction = paymentService.processUpiCallback(request);
        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    /**
     * Simulate customer approving UPI collect (for testing).
     * POST /api/transactions/{transactionId}/simulate-approval
     */
    @PostMapping("/{transactionId}/simulate-approval")
    public ResponseEntity<TransactionResponse> simulateApproval(
            @PathVariable String transactionId,
            @RequestParam(defaultValue = "success@upi") String customerVpa) {
        Transaction transaction = paymentService.simulateCustomerApproval(transactionId, customerVpa);
        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setTransactionUuid(transaction.getTransactionUuid());
        response.setPaymentId(transaction.getPayment().getPaymentId());
        response.setTxnType(transaction.getTxnType());
        response.setPaymentMethod(transaction.getPaymentMethod());
        response.setAmount(transaction.getAmount());
        response.setStatus(transaction.getStatus());
        response.setPspReferenceId(transaction.getPspReferenceId());
        response.setBankReferenceId(transaction.getBankReferenceId());
        response.setFailureReason(transaction.getFailureReason());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}
