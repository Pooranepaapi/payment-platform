package org.personal.simulator.dto.upi;

import jakarta.validation.constraints.NotBlank;
import org.personal.simulator.dto.common.SimulatorRequest;

/**
 * UPI Refund request.
 */
public class UpiRefundSimulatorRequest extends SimulatorRequest {

    @NotBlank(message = "Original transaction ID is required")
    private String originalTransactionId;

    @NotBlank(message = "Original PSP reference is required")
    private String originalPspReferenceId;

    private String reason;

    // Getters and setters
    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public String getOriginalPspReferenceId() {
        return originalPspReferenceId;
    }

    public void setOriginalPspReferenceId(String originalPspReferenceId) {
        this.originalPspReferenceId = originalPspReferenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
