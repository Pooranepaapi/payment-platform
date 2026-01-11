package org.personal.entity;

import jakarta.persistence.*;
import org.personal.enums.MerchantStatus;

import java.time.LocalDateTime;

/**
 * Merchant Entity (Extended for QR Payment System)
 *
 * Represents a child merchant/store under a MasterMerchant.
 * Supports both legacy UPI and new QR payment systems.
 *
 * Phase-1 (UPI): merchant_id field (original)
 * Phase-1 (QR): master_merchant_id + merchant_uuid fields (new)
 *
 * Both systems coexist in same database.
 */
@Entity
@Table(name = "merchant", indexes = {
    @Index(name = "idx_merchant_uuid", columnList = "merchant_uuid", unique = true),
    @Index(name = "idx_merchant_master", columnList = "master_merchant_id")
})
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Legacy UPI System ====================

    @Column(name = "merchant_id", unique = true, nullable = false)
    private String merchantId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile")
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    // ==================== QR Payment System ====================

    /**
     * Parent MasterMerchant (for QR system)
     * - Many-to-One relationship
     * - Foreign key: master_merchant_id
     * - For legacy UPI merchants, this can be null or point to default master
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_merchant_id")
    private MasterMerchant masterMerchant;

    /**
     * Merchant UUID for external reference (QR system)
     * - Unique globally
     * - Safe to share with customers
     * - Used in APIs: /api/v1/merchants/{merchantUuid}
     */
    @Column(name = "merchant_uuid", length = 36, unique = true)
    private String merchantUuid;

    /**
     * Merchant's UPI VPA (Virtual Payment Address)
     * - Format: username@bankname
     * - Example: "store123@axis"
     * - Unique per merchant
     */
    @Column(name = "vpa", length = 50, unique = true)
    private String vpa;

    /**
     * Platform fee percentage for this merchant
     * - Default: 2.50%
     * - Decimal(5,2): Range 0.00 to 999.99
     * - Frozen at payment creation time (immutable snapshot)
     */
    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal platformFeePercentage = new java.math.BigDecimal("2.50");

    // ==================== Timestamps ====================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public void setStatus(MerchantStatus status) {
        this.status = status;
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

    // ==================== QR System Getters/Setters ====================

    public MasterMerchant getMasterMerchant() {
        return masterMerchant;
    }

    public void setMasterMerchant(MasterMerchant masterMerchant) {
        this.masterMerchant = masterMerchant;
    }

    public String getMerchantUuid() {
        return merchantUuid;
    }

    public void setMerchantUuid(String merchantUuid) {
        this.merchantUuid = merchantUuid;
    }

    public String getVpa() {
        return vpa;
    }

    public void setVpa(String vpa) {
        this.vpa = vpa;
    }

    public java.math.BigDecimal getPlatformFeePercentage() {
        return platformFeePercentage;
    }

    public void setPlatformFeePercentage(java.math.BigDecimal platformFeePercentage) {
        this.platformFeePercentage = platformFeePercentage;
    }
}
