package org.personal.simulator.service;

import org.personal.simulator.dto.upi.*;
import org.personal.simulator.enums.PaymentMethod;
import org.personal.simulator.enums.PaymentType;
import org.personal.simulator.enums.SimulatorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Abstract base class for all UPI bank simulators.
 * Provides common logic for VPA behavior mapping and reference ID generation.
 */
public abstract class AbstractUpiSimulator implements UpiSimulator {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Random random = new Random();
    protected final CallbackService callbackService;

    // Base test VPA behaviors - can be extended by subclasses
    protected static final Map<String, String> BASE_VPA_BEHAVIORS = Map.of(
        "success@upi", "SUCCESS",
        "fail@upi", "FAILED",
        "timeout@upi", "PENDING",
        "insufficient@upi", "INSUFFICIENT_FUNDS"
    );

    public AbstractUpiSimulator(CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == getPaymentType();
    }

    /**
     * Generate PSP reference ID with bank prefix.
     */
    protected String generatePspReferenceId() {
        return getBankPrefix() + "PSP" + UUID.randomUUID().toString()
            .replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * Generate bank reference ID with bank prefix and timestamp.
     */
    protected String generateBankReferenceId() {
        return getBankPrefix() + "BNK" + System.currentTimeMillis() + random.nextInt(1000);
    }

    /**
     * Generate UPI transaction ID.
     */
    protected String generateUpiTxnId() {
        return getBankPrefix() + System.currentTimeMillis() + random.nextInt(10000);
    }

    /**
     * Generate RRN (Retrieval Reference Number).
     */
    protected String generateRrn() {
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }

    /**
     * Get behavior for customer VPA. Checks bank-specific VPAs first, then base.
     */
    protected String getVpaBehavior(String customerVpa) {
        String normalized = customerVpa.toLowerCase();

        // Check bank-specific VPAs first
        Map<String, String> bankSpecific = getBankSpecificVpaBehaviors();
        if (bankSpecific.containsKey(normalized)) {
            return bankSpecific.get(normalized);
        }

        // Fall back to base behaviors
        return BASE_VPA_BEHAVIORS.getOrDefault(normalized, "SUCCESS");
    }

    /**
     * Subclasses provide their bank prefix (e.g., "RBL", "HDFC", "KOTAK").
     */
    protected abstract String getBankPrefix();

    /**
     * Subclasses can override to provide bank-specific VPA behaviors.
     */
    protected abstract Map<String, String> getBankSpecificVpaBehaviors();

    /**
     * Subclasses provide the callback delay in milliseconds.
     * Different banks can have different simulated response times.
     */
    protected abstract long getCallbackDelayMs();

    @Override
    public UpiCollectSimulatorResponse initiateCollect(UpiCollectSimulatorRequest request) {
        log.info("[{}] Initiating collect: txnId={}, customerVpa={}, amount={}",
            getBankPrefix(), request.getTransactionId(),
            request.getCustomerVpa(), request.getAmount());

        String pspReferenceId = generatePspReferenceId();
        String behavior = getVpaBehavior(request.getCustomerVpa());

        UpiCollectSimulatorResponse response = new UpiCollectSimulatorResponse();
        response.setTransactionId(request.getTransactionId());
        response.setPspReferenceId(pspReferenceId);
        response.setBankCode(getBankPrefix());
        response.setUpiTxnId(generateUpiTxnId());

        // Immediate response is always PENDING (async flow)
        response.setStatus(SimulatorStatus.PENDING);
        response.setMessage("Collect request initiated. Awaiting customer approval.");

        // Schedule async callback (except for timeout VPA)
        if (!"PENDING".equals(behavior)) {
            scheduleCallback(request, pspReferenceId, behavior);
        } else {
            log.info("[{}] Timeout VPA detected, no callback will be sent", getBankPrefix());
        }

        log.info("[{}] Collect initiated: pspRef={}, upiTxnId={}",
            getBankPrefix(), pspReferenceId, response.getUpiTxnId());
        return response;
    }

    @Override
    public UpiRefundSimulatorResponse initiateRefund(UpiRefundSimulatorRequest request) {
        log.info("[{}] Initiating refund: txnId={}, originalTxnId={}, amount={}",
            getBankPrefix(), request.getTransactionId(),
            request.getOriginalTransactionId(), request.getAmount());

        String pspReferenceId = generatePspReferenceId();
        String bankReferenceId = generateBankReferenceId();

        UpiRefundSimulatorResponse response = new UpiRefundSimulatorResponse();
        response.setTransactionId(request.getTransactionId());
        response.setOriginalTransactionId(request.getOriginalTransactionId());
        response.setPspReferenceId(pspReferenceId);
        response.setBankReferenceId(bankReferenceId);
        response.setBankCode(getBankPrefix());
        response.setRefundRrn(generateRrn());

        // Refunds are processed immediately in test mode, then callback
        response.setStatus(SimulatorStatus.PENDING);
        response.setMessage("Refund initiated. Processing...");

        // Schedule async callback for refund completion
        scheduleRefundCallback(request, pspReferenceId, bankReferenceId);

        log.info("[{}] Refund initiated: pspRef={}, bankRef={}",
            getBankPrefix(), pspReferenceId, bankReferenceId);
        return response;
    }

    @Override
    public UpiCollectSimulatorResponse checkStatus(UpiStatusCheckRequest request) {
        log.info("[{}] Checking status: pspRef={}", getBankPrefix(), request.getPspReferenceId());

        // In real scenario, we'd look up state. For stateless simulator,
        // return success for non-timeout VPAs
        UpiCollectSimulatorResponse response = new UpiCollectSimulatorResponse();
        response.setTransactionId(request.getTransactionId());
        response.setPspReferenceId(request.getPspReferenceId());
        response.setBankReferenceId(generateBankReferenceId());
        response.setStatus(SimulatorStatus.SUCCESS);
        response.setBankCode(getBankPrefix());
        response.setRrn(generateRrn());
        response.setMessage("Transaction found and successful");

        return response;
    }

    /**
     * Schedule async callback to backend after simulated delay.
     */
    protected void scheduleCallback(UpiCollectSimulatorRequest request,
                                    String pspReferenceId, String behavior) {
        SimulatorStatus finalStatus;
        String failureReason = null;

        switch (behavior) {
            case "FAILED" -> {
                finalStatus = SimulatorStatus.FAILED;
                failureReason = "Transaction declined by customer";
            }
            case "INSUFFICIENT_FUNDS" -> {
                finalStatus = SimulatorStatus.FAILED;
                failureReason = "Insufficient funds in customer account";
            }
            default -> finalStatus = SimulatorStatus.SUCCESS;
        }

        log.info("[{}] Scheduling callback: txnId={}, finalStatus={}, delay={}ms",
            getBankPrefix(), request.getTransactionId(), finalStatus, getCallbackDelayMs());

        callbackService.scheduleCallback(
            request.getTransactionId(),
            pspReferenceId,
            generateBankReferenceId(),
            finalStatus,
            failureReason,
            request.getCallbackUrl(),
            getCallbackDelayMs()
        );
    }

    protected void scheduleRefundCallback(UpiRefundSimulatorRequest request,
                                          String pspReferenceId, String bankReferenceId) {
        log.info("[{}] Scheduling refund callback: txnId={}, delay={}ms",
            getBankPrefix(), request.getTransactionId(), getCallbackDelayMs());

        callbackService.scheduleCallback(
            request.getTransactionId(),
            pspReferenceId,
            bankReferenceId,
            SimulatorStatus.SUCCESS,
            null,
            request.getCallbackUrl(),
            getCallbackDelayMs()
        );
    }
}
