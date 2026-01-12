package org.personal.simulator.dto.upi;

import org.personal.simulator.dto.common.SimulatorResponse;

/**
 * UPI Refund response.
 */
public class UpiRefundSimulatorResponse extends SimulatorResponse {

    private String originalTransactionId;
    private String refundRrn;

    // Getters and setters
    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public String getRefundRrn() {
        return refundRrn;
    }

    public void setRefundRrn(String refundRrn) {
        this.refundRrn = refundRrn;
    }
}
