package org.personal.controller;

import jakarta.validation.Valid;
import org.personal.dto.*;
import org.personal.entity.Payment;
import org.personal.entity.Transaction;
import org.personal.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create a new payment.
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.ok(toPaymentResponse(payment));
    }

    /**
     * Get payment by ID.
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(toPaymentResponse(payment));
    }

    /**
     * Initiate UPI collect for a payment.
     * POST /api/payments/upi/collect
     */
    @PostMapping("/upi/collect")
    public ResponseEntity<TransactionResponse> initiateUpiCollect(@Valid @RequestBody UpiCollectRequest request) {
        Transaction transaction = paymentService.initiateUpiCollect(request);
        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    /**
     * Get all transactions for a payment.
     * GET /api/payments/{paymentId}/transactions
     */
    @GetMapping("/{paymentId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getPaymentTransactions(@PathVariable String paymentId) {
        List<Transaction> transactions = paymentService.getTransactionsForPayment(paymentId);
        List<TransactionResponse> responses = transactions.stream()
                .map(this::toTransactionResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Initiate refund for a payment.
     * POST /api/payments/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> initiateRefund(@Valid @RequestBody RefundRequest request) {
        Transaction transaction = paymentService.initiateRefund(request);
        return ResponseEntity.ok(toTransactionResponse(transaction));
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getPaymentId());
        response.setPaymentUuid(payment.getPaymentUuid());
        response.setMerchantOrderId(payment.getMerchantOrderId());
        response.setDueAmount(payment.getDueAmount());
        response.setPaidAmount(payment.getPaidAmount());
        response.setRefundedAmount(payment.getRefundedAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setTestMode(payment.getTestMode());
        response.setExpiresAt(payment.getExpiresAt());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
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
