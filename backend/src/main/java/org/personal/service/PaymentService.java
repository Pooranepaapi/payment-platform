package org.personal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.personal.dto.*;
import org.personal.entity.Contract;
import org.personal.entity.Merchant;
import org.personal.entity.Payment;
import org.personal.entity.Transaction;
import org.personal.enums.*;
import org.personal.exception.InvalidOperationException;
import org.personal.exception.PaymentException;
import org.personal.exception.ResourceNotFoundException;
import org.personal.repository.ContractRepository;
import org.personal.repository.MerchantRepository;
import org.personal.repository.PaymentRepository;
import org.personal.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final MerchantRepository merchantRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final UpiSimulatorService upiSimulatorService;
    private final ObjectMapper objectMapper;

    public PaymentService(MerchantRepository merchantRepository,
                          ContractRepository contractRepository,
                          PaymentRepository paymentRepository,
                          TransactionRepository transactionRepository,
                          UpiSimulatorService upiSimulatorService,
                          ObjectMapper objectMapper) {
        this.merchantRepository = merchantRepository;
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.upiSimulatorService = upiSimulatorService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new payment.
     */
    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for merchant={}, amount={}", request.getMerchantId(), request.getAmount());

        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", request.getMerchantId()));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new PaymentException("Merchant is not active", "MERCHANT_INACTIVE");
        }

        Payment payment = new Payment();
        payment.setPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setPaymentUuid(UUID.randomUUID().toString());
        payment.setMerchant(merchant);
        payment.setMerchantOrderId(request.getMerchantOrderId());
        payment.setDueAmount(request.getAmount());
        payment.setPaidAmount(BigDecimal.ZERO);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
        payment.setTestMode(request.getTestMode() != null ? request.getTestMode() : true);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCustomerMobile(request.getCustomerMobile());
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        payment = paymentRepository.save(payment);
        log.info("Payment created: {}", payment.getPaymentId());

        return payment;
    }

    /**
     * Initiates a UPI collect transaction for a payment.
     */
    @Transactional
    public Transaction initiateUpiCollect(UpiCollectRequest request) {
        log.info("Initiating UPI collect for payment={}, customerVpa={}",
                request.getPaymentId(), request.getCustomerVpa());

        Payment payment = paymentRepository.findByPaymentId(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", request.getPaymentId()));

        validatePaymentForTransaction(payment);

        // Get merchant's UPI contract
        Contract contract = findUpiContract(payment.getMerchant(), request.getContractId());

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId("txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        transaction.setTransactionUuid(UUID.randomUUID().toString());
        transaction.setPayment(payment);
        transaction.setContract(contract);
        transaction.setTxnType(TransactionType.DEBIT);
        transaction.setPaymentMethod(PaymentMethod.UPI);
        transaction.setAmount(payment.getDueAmount().subtract(payment.getPaidAmount()));
        transaction.setTestMode(payment.getTestMode());
        transaction.setStatus(TransactionStatus.INITIATED);

        // Store request payload
        try {
            Map<String, Object> requestPayload = Map.of(
                "customerVpa", request.getCustomerVpa(),
                "merchantVpa", getMerchantVpaFromContract(contract),
                "amount", transaction.getAmount()
            );
            transaction.setRequestPayload(objectMapper.writeValueAsString(requestPayload));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize request payload", e);
        }

        transaction = transactionRepository.save(transaction);

        // Update payment status to PENDING
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // If test mode, use simulator
        if (payment.getTestMode()) {
            String merchantVpa = getMerchantVpaFromContract(contract);
            UpiSimulatorService.SimulatorResponse simResponse =
                    upiSimulatorService.initiateCollect(transaction, request.getCustomerVpa(), merchantVpa);

            transaction.setPspReferenceId(simResponse.getPspReferenceId());
            transaction.setStatus(simResponse.getStatus());
            transaction.setFailureReason(simResponse.getFailureReason());

            try {
                transaction.setResponsePayload(objectMapper.writeValueAsString(Map.of(
                    "pspReferenceId", simResponse.getPspReferenceId(),
                    "status", simResponse.getStatus().name(),
                    "failureReason", simResponse.getFailureReason() != null ? simResponse.getFailureReason() : ""
                )));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize response payload", e);
            }

            transaction = transactionRepository.save(transaction);
        }

        log.info("UPI collect initiated: txn={}, status={}",
                transaction.getTransactionId(), transaction.getStatus());

        return transaction;
    }

    /**
     * Processes UPI callback (simulates customer approval/rejection).
     */
    @Transactional
    public Transaction processUpiCallback(UpiCallbackRequest request) {
        log.info("Processing UPI callback for txn={}", request.getTransactionId());

        Transaction transaction = transactionRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", request.getTransactionId()));

        if (transaction.getStatus() != TransactionStatus.PENDING
                && transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new InvalidOperationException(
                "Transaction is not in a state that can receive callbacks",
                "INVALID_TRANSACTION_STATE");
        }

        // Update transaction
        TransactionStatus newStatus = TransactionStatus.valueOf(request.getStatus());
        transaction.setStatus(newStatus);
        transaction.setBankReferenceId(request.getBankReferenceId());
        if (request.getFailureReason() != null) {
            transaction.setFailureReason(request.getFailureReason());
        }

        transaction = transactionRepository.save(transaction);

        // Update payment based on transaction result
        Payment payment = transaction.getPayment();
        updatePaymentAfterTransaction(payment, transaction);

        log.info("UPI callback processed: txn={}, newStatus={}",
                transaction.getTransactionId(), newStatus);

        return transaction;
    }

    /**
     * Simulates customer approving the UPI collect request.
     */
    @Transactional
    public Transaction simulateCustomerApproval(String transactionId, String customerVpa) {
        log.info("Simulating customer approval for txn={}", transactionId);

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidOperationException(
                "Transaction is not pending approval",
                "INVALID_TRANSACTION_STATE");
        }

        UpiSimulatorService.SimulatorResponse simResponse =
                upiSimulatorService.simulateCallback(transaction, customerVpa);

        transaction.setStatus(simResponse.getStatus());
        transaction.setBankReferenceId(simResponse.getBankReferenceId());
        transaction.setFailureReason(simResponse.getFailureReason());

        transaction = transactionRepository.save(transaction);

        // Update payment
        Payment payment = transaction.getPayment();
        updatePaymentAfterTransaction(payment, transaction);

        log.info("Customer approval simulated: txn={}, result={}",
                transactionId, simResponse.getStatus());

        return transaction;
    }

    /**
     * Initiates a refund.
     */
    @Transactional
    public Transaction initiateRefund(RefundRequest request) {
        log.info("Initiating refund for payment={}, amount={}",
                request.getPaymentId(), request.getAmount());

        Payment payment = paymentRepository.findByPaymentId(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", request.getPaymentId()));

        validatePaymentForRefund(payment, request.getAmount());

        // Find the original successful DEBIT transaction
        List<Transaction> successfulDebits = transactionRepository
                .findByPaymentAndTxnType(payment, TransactionType.DEBIT)
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .toList();

        if (successfulDebits.isEmpty()) {
            throw new InvalidOperationException(
                "No successful payment transaction found to refund",
                "NO_TRANSACTION_TO_REFUND");
        }

        Transaction originalTransaction = successfulDebits.get(0);

        // Create refund transaction
        Transaction refundTransaction = new Transaction();
        refundTransaction.setTransactionId("txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        refundTransaction.setPayment(payment);
        refundTransaction.setContract(originalTransaction.getContract());
        refundTransaction.setTxnType(TransactionType.CREDIT);
        refundTransaction.setPaymentMethod(originalTransaction.getPaymentMethod());
        refundTransaction.setAmount(request.getAmount());
        refundTransaction.setTestMode(payment.getTestMode());
        refundTransaction.setStatus(TransactionStatus.INITIATED);

        refundTransaction = transactionRepository.save(refundTransaction);

        // If test mode, use simulator
        if (payment.getTestMode()) {
            UpiSimulatorService.SimulatorResponse simResponse =
                    upiSimulatorService.initiateRefund(originalTransaction, refundTransaction);

            refundTransaction.setPspReferenceId(simResponse.getPspReferenceId());
            refundTransaction.setBankReferenceId(simResponse.getBankReferenceId());
            refundTransaction.setStatus(simResponse.getStatus());
            refundTransaction.setFailureReason(simResponse.getFailureReason());

            refundTransaction = transactionRepository.save(refundTransaction);

            // Update payment refunded amount
            if (simResponse.getStatus() == TransactionStatus.SUCCESS) {
                payment.setRefundedAmount(payment.getRefundedAmount().add(request.getAmount()));
                updatePaymentStatusAfterRefund(payment);
                paymentRepository.save(payment);
            }
        }

        log.info("Refund initiated: txn={}, status={}",
                refundTransaction.getTransactionId(), refundTransaction.getStatus());

        return refundTransaction;
    }

    /**
     * Gets payment by ID.
     */
    public Payment getPayment(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
    }

    /**
     * Gets transaction by ID.
     */
    public Transaction getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
    }

    /**
     * Gets all transactions for a payment.
     */
    public List<Transaction> getTransactionsForPayment(String paymentId) {
        Payment payment = getPayment(paymentId);
        return transactionRepository.findByPayment(payment);
    }

    // Private helper methods

    private void validatePaymentForTransaction(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidOperationException("Payment is already completed", "PAYMENT_COMPLETED");
        }
        if (payment.getStatus() == PaymentStatus.EXPIRED) {
            throw new InvalidOperationException("Payment has expired", "PAYMENT_EXPIRED");
        }
        if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            throw new InvalidOperationException("Payment has expired", "PAYMENT_EXPIRED");
        }
    }

    private void validatePaymentForRefund(Payment payment, BigDecimal refundAmount) {
        if (payment.getStatus() != PaymentStatus.SUCCESS
                && payment.getStatus() != PaymentStatus.REFUNDED_PARTIALLY) {
            throw new InvalidOperationException(
                "Payment must be successful to initiate refund",
                "INVALID_PAYMENT_STATE");
        }

        BigDecimal refundableAmount = payment.getPaidAmount().subtract(payment.getRefundedAmount());
        if (refundAmount.compareTo(refundableAmount) > 0) {
            throw new InvalidOperationException(
                "Refund amount exceeds refundable amount: " + refundableAmount,
                "REFUND_AMOUNT_EXCEEDED");
        }
    }

    private Contract findUpiContract(Merchant merchant, String contractId) {
        if (contractId != null) {
            Contract contract = contractRepository.findByContractId(contractId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contract", contractId));
            if (!contract.getMerchant().getId().equals(merchant.getId())) {
                throw new PaymentException("Contract does not belong to merchant", "INVALID_CONTRACT");
            }
            return contract;
        }

        // Find first active UPI contract
        List<Contract> upiContracts = contractRepository
                .findByMerchantAndPaymentMethodAndStatus(merchant, PaymentMethod.UPI, ContractStatus.ACTIVE);

        if (upiContracts.isEmpty()) {
            throw new PaymentException("No active UPI contract found for merchant", "NO_UPI_CONTRACT");
        }

        return upiContracts.get(0);
    }

    private String getMerchantVpaFromContract(Contract contract) {
        try {
            if (contract.getParams() != null) {
                Map<String, Object> params = objectMapper.readValue(contract.getParams(), Map.class);
                return (String) params.getOrDefault("merchantVpa", "merchant@upi");
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse contract params", e);
        }
        return "merchant@upi";
    }

    private void updatePaymentAfterTransaction(Payment payment, Transaction transaction) {
        if (transaction.getTxnType() == TransactionType.DEBIT) {
            if (transaction.getStatus() == TransactionStatus.SUCCESS) {
                payment.setPaidAmount(payment.getPaidAmount().add(transaction.getAmount()));
                if (payment.getPaidAmount().compareTo(payment.getDueAmount()) >= 0) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                }
            } else if (transaction.getStatus() == TransactionStatus.FAILED) {
                // Check if there are any pending transactions
                List<Transaction> pendingTxns = transactionRepository.findByPaymentAndStatus(payment, TransactionStatus.PENDING);
                if (pendingTxns.isEmpty()) {
                    payment.setStatus(PaymentStatus.FAILED);
                }
            }
        }
        paymentRepository.save(payment);
    }

    private void updatePaymentStatusAfterRefund(Payment payment) {
        if (payment.getRefundedAmount().compareTo(payment.getPaidAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else if (payment.getRefundedAmount().compareTo(BigDecimal.ZERO) > 0) {
            payment.setStatus(PaymentStatus.REFUNDED_PARTIALLY);
        }
    }
}
