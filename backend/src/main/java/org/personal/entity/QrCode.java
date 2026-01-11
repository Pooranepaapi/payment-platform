package org.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.personal.enums.QrType;

import java.time.LocalDateTime;

/**
 * QrCode Entity
 *
 * Stores generated QR code images and metadata for a payment.
 * 1:1 relationship with PaymentOrder.
 *
 * Generated when PaymentOrder status transitions to QR_GENERATED.
 * Contains both:
 * - Base64 PNG image (for app display)
 * - SVG image (for scaling and printing)
 * - UPI intent URI (for manual entry fallback)
 *
 * Relationship:
 * - One-to-One: One PaymentOrder has exactly one QrCode
 *
 * Design:
 * - QR images stored as LONGTEXT (Base64 encoded)
 * - UPI intent string for manual entry by customer
 * - Expires same time as payment (expiresAt copied from PaymentOrder)
 */
@Entity
@Table(
    name = "qr_code",
    indexes = {
        @Index(name = "idx_qr_payment", columnList = "payment_order_id", unique = true)
    }
)
@Getter
@Setter
public class QrCode {

    // ==================== Primary Key ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Payment Reference ====================

    /**
     * Which payment does this QR code belong to?
     * - One-to-One relationship (unique constraint)
     * - Foreign key: payment_order_id
     * - Each payment has exactly one QR code
     * - If payment deleted (cascade), QR also deleted
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false, unique = true)
    private PaymentOrder paymentOrder;

    // ==================== QR Code Type ====================

    /**
     * Type of QR code (DYNAMIC, STATIC)
     * - DYNAMIC: Generated per payment, includes amount
     * - STATIC: Generated once per merchant, no amount (Phase-2)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "qr_type", nullable = false, length = 50)
    private QrType qrType = QrType.DYNAMIC;

    // ==================== QR Images ====================

    /**
     * QR code as Base64-encoded PNG image
     * - Format: data:image/png;base64,[BASE64_DATA]
     * - Can be directly used in HTML <img src="...">
     * - ~2-3 KB for typical payment QR
     * - LONGTEXT: Can store very large encoded images
     * - Used by mobile apps for display
     */
    @Column(name = "qr_image_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String qrImageBase64;

    /**
     * QR code as SVG (Scalable Vector Graphics)
     * - XML text format
     * - Can be scaled to any size without quality loss
     * - Better for printing
     * - Used by websites for flexible rendering
     * - ~5-10 KB for typical payment QR
     */
    @Column(name = "qr_image_svg", nullable = false, columnDefinition = "LONGTEXT")
    private String qrImageSvg;

    // ==================== UPI Intent ====================

    /**
     * UPI intent string (fallback manual entry)
     * Format: upi://pay?pa=storea@axis&pn=Store%20A&am=100.50&tr=ORDER-123&tn=Coffee
     *
     * Fields:
     * - pa: Merchant UPI address (VPA)
     * - pn: Merchant name (URL encoded)
     * - am: Amount in rupees (decimal)
     * - tr: Transaction reference (optional)
     * - tn: Transaction note/description (URL encoded, optional)
     *
     * Used when:
     * - QR scanning fails
     * - Manual UPI app launch
     * - UPI link sharing
     *
     * Max length: 500 characters (typical: ~100-150)
     */
    @Column(name = "upi_intent", nullable = false, length = 500)
    private String upiIntent;

    // ==================== Expiry ====================

    /**
     * When this QR code expires
     * - Copied from PaymentOrder.expiresAt
     * - Typically 15 minutes from creation
     * - After this time, QR is no longer valid
     * - Customer can't pay via this QR
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // ==================== Timestamps ====================

    /**
     * When QR code was generated
     * - Immutable (updatable = false)
     * - Set when PaymentOrder status → QR_GENERATED
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last modified timestamp
     * - Updated if QR is regenerated (rare)
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== Lifecycle Callbacks ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentOrder getPaymentOrder() {
        return paymentOrder;
    }

    public void setPaymentOrder(PaymentOrder paymentOrder) {
        this.paymentOrder = paymentOrder;
    }

    public QrType getQrType() {
        return qrType;
    }

    public void setQrType(QrType qrType) {
        this.qrType = qrType;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== Helper Methods ====================

    /**
     * Check if this QR code has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "QrCode{" +
                "id=" + id +
                ", paymentOrderId=" + (paymentOrder != null ? paymentOrder.getId() : null) +
                ", qrType=" + qrType +
                ", expiresAt=" + expiresAt +
                ", isExpired=" + isExpired() +
                ", base64Length=" + (qrImageBase64 != null ? qrImageBase64.length() : 0) +
                '}';
    }
}
