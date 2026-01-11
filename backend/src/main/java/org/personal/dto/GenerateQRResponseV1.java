package org.personal.dto;

import org.personal.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for QR code generation
 * POST /api/v1/payments/{paymentId}/qr
 */
public class GenerateQRResponseV1 {

    private Long paymentId;
    private String qrImageBase64;
    private String qrImageSvg;
    private String upiIntent;
    private LocalDateTime expiresAt;
    private PaymentStatus status;
    private String message;

    // Getters and Setters
    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getQrImageBase64() {
        return qrImageBase64;
    }

    public void setQrImageBase64(String qrImageBase64) {
        this.qrImageBase64 = qrImageBase64;
    }

    public String getQrImageSvg() {
        return qrImageSvg;
    }

    public void setQrImageSvg(String qrImageSvg) {
        this.qrImageSvg = qrImageSvg;
    }

    public String getUpiIntent() {
        return upiIntent;
    }

    public void setUpiIntent(String upiIntent) {
        this.upiIntent = upiIntent;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
