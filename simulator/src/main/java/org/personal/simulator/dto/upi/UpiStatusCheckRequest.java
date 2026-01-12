package org.personal.simulator.dto.upi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.personal.simulator.enums.PaymentType;

/**
 * Request for checking UPI transaction status.
 */
public class UpiStatusCheckRequest {

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @NotBlank(message = "PSP reference ID is required")
    private String pspReferenceId;

    private String transactionId;

    // Getters and setters
    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public String getPspReferenceId() {
        return pspReferenceId;
    }

    public void setPspReferenceId(String pspReferenceId) {
        this.pspReferenceId = pspReferenceId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
