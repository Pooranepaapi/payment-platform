package org.personal.simulator.dto.upi;

import org.personal.simulator.dto.common.SimulatorResponse;

/**
 * UPI Collect response with UPI-specific fields.
 */
public class UpiCollectSimulatorResponse extends SimulatorResponse {

    private String upiTxnId;
    private String rrn;
    private String approvalNumber;

    // Getters and setters
    public String getUpiTxnId() {
        return upiTxnId;
    }

    public void setUpiTxnId(String upiTxnId) {
        this.upiTxnId = upiTxnId;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }
}
