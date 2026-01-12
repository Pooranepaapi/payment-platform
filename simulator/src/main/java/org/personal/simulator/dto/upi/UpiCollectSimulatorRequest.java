package org.personal.simulator.dto.upi;

import jakarta.validation.constraints.NotBlank;
import org.personal.simulator.dto.common.SimulatorRequest;

/**
 * UPI Collect request with UPI-specific fields.
 */
public class UpiCollectSimulatorRequest extends SimulatorRequest {

    @NotBlank(message = "Customer VPA is required")
    private String customerVpa;

    @NotBlank(message = "Merchant VPA is required")
    private String merchantVpa;

    private String remarks;
    private String refUrl;
    private Integer expiryMinutes = 15;

    // Getters and setters
    public String getCustomerVpa() {
        return customerVpa;
    }

    public void setCustomerVpa(String customerVpa) {
        this.customerVpa = customerVpa;
    }

    public String getMerchantVpa() {
        return merchantVpa;
    }

    public void setMerchantVpa(String merchantVpa) {
        this.merchantVpa = merchantVpa;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRefUrl() {
        return refUrl;
    }

    public void setRefUrl(String refUrl) {
        this.refUrl = refUrl;
    }

    public Integer getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(Integer expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
}
