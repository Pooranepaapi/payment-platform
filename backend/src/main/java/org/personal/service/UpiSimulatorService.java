package org.personal.service;

import org.personal.entity.Transaction;
import org.personal.enums.TransactionStatus;
import org.personal.enums.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Simulates UPI PSP responses for test mode transactions.
 * In production, this would be replaced with actual PSP integrations.
 */
@Service
public class UpiSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(UpiSimulatorService.class);
    private final Random random = new Random();

    // Test VPAs and their behaviors
    private static final Map<String, String> TEST_VPA_BEHAVIORS = Map.of(
        "success@upi", "SUCCESS",
        "fail@upi", "FAILED",
        "timeout@upi", "PENDING",
        "insufficient@upi", "INSUFFICIENT_FUNDS"
    );

    /**
     * Simulates a UPI collect request initiation.
     * Returns a PSP reference ID for tracking.
     */
    public SimulatorResponse initiateCollect(Transaction transaction, String customerVpa, String merchantVpa) {
        log.info("UPI Simulator: Initiating collect request for txn={}, customerVpa={}, merchantVpa={}",
                transaction.getTransactionId(), customerVpa, merchantVpa);

        String pspReferenceId = "PSP" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // Check for test VPA behaviors
        String behavior = TEST_VPA_BEHAVIORS.getOrDefault(customerVpa.toLowerCase(), "SUCCESS");

        SimulatorResponse response = new SimulatorResponse();
        response.setPspReferenceId(pspReferenceId);

        switch (behavior) {
            case "FAILED" -> {
                response.setStatus(TransactionStatus.FAILED);
                response.setFailureReason("Customer rejected the collect request");
            }
            case "INSUFFICIENT_FUNDS" -> {
                response.setStatus(TransactionStatus.FAILED);
                response.setFailureReason("Insufficient funds in customer account");
            }
            case "PENDING" -> {
                response.setStatus(TransactionStatus.PENDING);
                response.setFailureReason(null);
            }
            default -> {
                // Simulate async - for demo we'll mark as PENDING, callback will mark SUCCESS
                response.setStatus(TransactionStatus.PENDING);
                response.setFailureReason(null);
            }
        }

        log.info("UPI Simulator: Collect initiated with pspRef={}, status={}",
                pspReferenceId, response.getStatus());

        return response;
    }

    /**
     * Simulates UPI callback - typically called after customer approves/rejects on their UPI app.
     * In real world, PSP would call our callback endpoint.
     */
    public SimulatorResponse simulateCallback(Transaction transaction, String customerVpa) {
        log.info("UPI Simulator: Processing callback for txn={}", transaction.getTransactionId());

        String behavior = TEST_VPA_BEHAVIORS.getOrDefault(customerVpa.toLowerCase(), "SUCCESS");
        String bankReferenceId = "BNK" + System.currentTimeMillis() + random.nextInt(1000);

        SimulatorResponse response = new SimulatorResponse();
        response.setPspReferenceId(transaction.getPspReferenceId());
        response.setBankReferenceId(bankReferenceId);

        switch (behavior) {
            case "FAILED" -> {
                response.setStatus(TransactionStatus.FAILED);
                response.setFailureReason("Transaction declined by customer");
            }
            case "INSUFFICIENT_FUNDS" -> {
                response.setStatus(TransactionStatus.FAILED);
                response.setFailureReason("Insufficient funds");
            }
            case "PENDING" -> {
                response.setStatus(TransactionStatus.PENDING);
                response.setFailureReason(null);
            }
            default -> {
                response.setStatus(TransactionStatus.SUCCESS);
                response.setFailureReason(null);
            }
        }

        log.info("UPI Simulator: Callback processed with bankRef={}, status={}",
                bankReferenceId, response.getStatus());

        return response;
    }

    /**
     * Simulates a refund request.
     */
    public SimulatorResponse initiateRefund(Transaction originalTransaction, Transaction refundTransaction) {
        log.info("UPI Simulator: Initiating refund for original txn={}, refund txn={}",
                originalTransaction.getTransactionId(), refundTransaction.getTransactionId());

        String pspReferenceId = "RFND" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String bankReferenceId = "RBNK" + System.currentTimeMillis();

        SimulatorResponse response = new SimulatorResponse();
        response.setPspReferenceId(pspReferenceId);
        response.setBankReferenceId(bankReferenceId);
        response.setStatus(TransactionStatus.SUCCESS);
        response.setFailureReason(null);

        log.info("UPI Simulator: Refund processed with pspRef={}, bankRef={}",
                pspReferenceId, bankReferenceId);

        return response;
    }

    /**
     * Simulates checking transaction status with PSP.
     */
    public SimulatorResponse checkStatus(Transaction transaction) {
        log.info("UPI Simulator: Checking status for txn={}", transaction.getTransactionId());

        SimulatorResponse response = new SimulatorResponse();
        response.setPspReferenceId(transaction.getPspReferenceId());
        response.setBankReferenceId(transaction.getBankReferenceId());

        // For pending transactions, randomly resolve them
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            if (random.nextInt(100) < 80) { // 80% success rate
                response.setStatus(TransactionStatus.SUCCESS);
                response.setBankReferenceId("BNK" + System.currentTimeMillis());
            } else {
                response.setStatus(TransactionStatus.FAILED);
                response.setFailureReason("Transaction timed out");
            }
        } else {
            response.setStatus(transaction.getStatus());
            response.setFailureReason(transaction.getFailureReason());
        }

        return response;
    }

    /**
     * Response from UPI simulator.
     */
    public static class SimulatorResponse {
        private String pspReferenceId;
        private String bankReferenceId;
        private TransactionStatus status;
        private String failureReason;

        public String getPspReferenceId() {
            return pspReferenceId;
        }

        public void setPspReferenceId(String pspReferenceId) {
            this.pspReferenceId = pspReferenceId;
        }

        public String getBankReferenceId() {
            return bankReferenceId;
        }

        public void setBankReferenceId(String bankReferenceId) {
            this.bankReferenceId = bankReferenceId;
        }

        public TransactionStatus getStatus() {
            return status;
        }

        public void setStatus(TransactionStatus status) {
            this.status = status;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }
    }
}
