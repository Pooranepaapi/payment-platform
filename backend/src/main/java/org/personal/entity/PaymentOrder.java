package org.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.personal.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PaymentOrder Entity (Phase-1 QR Payment System)
 *
 * Represents a payment request with an associated QR code.
 * This is an immutable snapshot of a payment at the moment of creation.
 *
 * Design Principles:
 * - Immutable snapshot: Fees, amount, merchant details frozen at creation
 * - UUID-based: paymentUuid is external reference for customer/webhooks
 * - Status machine: Only valid transitions allowed
 * - Expiry: QR codes expire (15 min default), checked on-read
 * - Audit trail: All state changes logged in PaymentEvent table
 *
 * Relationships:
 * - Many-to-One: Belongs to Merchant
 * - One-to-One: Has one QrCode
 * - One-to-Many: Has multiple PaymentTransaction attempts
 * - One-to-Many: Has multiple PaymentEvent (audit log)
 * - One-to-Many: Has multiple RefundOrder (partial refunds supported)
 *
 * State Machine (Valid Transitions):
 * CREATED
 *   └─→ QR_GENERATED (backend generates QR)
 *   └─→ CANCELLED (merchant cancels)
 *
 * QR_GENERATED
 *   └─→ PENDING (customer scans & approves)
 *   └─→ EXPIRED (15 minutes pass, checked on-read)
 *   └─→ CANCELLED (merchant cancels)
 *
 * PENDING
 *   └─→ SUCCESS (PSP confirms)
 *   └─→ FAILED (PSP declines)
 *   └─→ EXPIRED (15 minutes pass, checked on-read)
 *
 * SUCCESS / FAILED / EXPIRED / CANCELLED
 *   └─→ (TERMINAL - no further changes)
 */
@Entity
@Table(
    name = "payment_order",
    indexes = {
        @Index(name = "idx_payment_uuid", columnList = "payment_uuid", unique = true),
        @Index(name = "idx_payment_main", columnList = "merchant_id,status,created_at"),
        @Index(name = "idx_payment_expires", columnList = "expires_at,status"),
        @Index(name = "idx_payment_created", columnList = "created_at DESC")
    }
)
@Getter
@Setter
public class PaymentOrder {

    // ==================== Primary Key ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== External References ====================

    /**
     * Payment UUID for external reference (customer links, webhooks, APIs)
     * - Unique globally
     * - Safe to share with customers
     * - Used in /api/v1/payments/{paymentId}
     * - Used in webhook notifications
     */
    @Column(name = "payment_uuid", nullable = false, unique = true, length = 36)
    private String paymentUuid = UUID.randomUUID().toString();

    // ==================== Merchant Reference ====================

    /**
     * Which merchant created this payment
     * - Many-to-One relationship
     * - Foreign key: merchant_id
     * - Must be active merchant
     * - Never null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    // ==================== Payment Status ====================

    /**
     * Current payment status
     * Follows state machine rules (see class documentation)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.CREATED;

    // ==================== Amount & Currency (Immutable Snapshot) ====================

    /**
     * Amount in paise (no floating-point errors)
     * - ₹100.50 = 10050 paise
     * - BIGINT for precision
     * - Must be > 0
     * - Immutable after creation
     */
    @Column(name = "amount_in_paise", nullable = false)
    private Long amountInPaise;

    /**
     * Currency code (ISO 4217)
     * - Default: "INR" (Indian Rupee)
     * - Support: INR, USD, EUR, GBP, JPY
     * - 3 characters, uppercase
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    // ==================== Fee Calculation (Immutable Snapshot) ====================

    /**
     * Platform fee taken (in paise)
     * - Calculated at payment creation time
     * - Frozen for audit trail & reconciliation
     * - Never changes after creation
     * Example: 10050 paise × 2.5% = 251 paise
     */
    @Column(name = "platform_fee_in_paise", nullable = false)
    private Long platformFeeInPaise;

    /**
     * Merchant's net amount after fee (in paise)
     * - amountInPaise - platformFeeInPaise
     * - What merchant actually receives
     */
    @Column(name = "merchant_net_in_paise", nullable = false)
    private Long merchantNetInPaise;

    // ==================== Additional Metadata ====================

    /**
     * Merchant's external order ID (optional)
     * - For merchant's internal reference
     * - Example: "ORDER-20260110-12345"
     */
    @Column(name = "external_order_id", length = 255)
    private String externalOrderId;

    /**
     * Payment description/purpose (optional)
     * - What is this payment for?
     * - Displayed to customer on UPI screen
     * - Example: "Coffee purchase"
     */
    @Column(name = "description", length = 255)
    private String description;

    // ==================== Expiry Logic ====================

    /**
     * When this QR code expires (and payment must fail)
     * - Typically: createdAt + 15 minutes
     * - Checked on every GET /api/v1/payments/{paymentId}
     * - If now > expiresAt && status not terminal → status = EXPIRED
     * - On-read evaluation (no cron job needed)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // ==================== Timestamps ====================

    /**
     * When payment was created
     * - Immutable (updatable = false)
     * - Used for expiry calculation
     * - Audit trail
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last modified timestamp
     * - Updated whenever status or amount changes
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== Relationships ====================

    /**
     * Associated QR code for this payment (1:1 relationship)
     * - One PaymentOrder has exactly one QrCode
     * - QrCode deleted when PaymentOrder deleted (cascade)
     * - Contains: Base64 image, SVG, UPI intent
     */
    @OneToOne(mappedBy = "paymentOrder", fetch = FetchType.LAZY)
    private QrCode qrCode;

    /**
     * Payment transaction attempts (1:Many relationship)
     * - One PaymentOrder can have multiple transaction attempts
     * - Example: First attempt fails, second attempt succeeds
     * - Tracks PSP interactions
     */
    @OneToMany(mappedBy = "paymentOrder", fetch = FetchType.LAZY)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    /**
     * Immutable audit trail (1:Many relationship)
     * - Append-only log of all state changes
     * - Example: CREATED → QR_GENERATED → PENDING → SUCCESS
     * - Never updated, only added to
     */
    @OneToMany(mappedBy = "paymentOrder", fetch = FetchType.LAZY)
    private List<PaymentEvent> events = new ArrayList<>();

    /**
     * Refund orders for this payment (1:Many relationship)
     * - One payment can have multiple refunds (partial refunds)
     * - Example: ₹100 payment → ₹50 refund + ₹50 refund
     * - Each refund tracked separately
     */
    @OneToMany(mappedBy = "paymentOrder", fetch = FetchType.LAZY)
    private List<RefundOrder> refunds = new ArrayList<>();

    // ==================== Lifecycle Callbacks ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (paymentUuid == null) {
            paymentUuid = UUID.randomUUID().toString();
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

    public String getPaymentUuid() {
        return paymentUuid;
    }

    public void setPaymentUuid(String paymentUuid) {
        this.paymentUuid = paymentUuid;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Long getAmountInPaise() {
        return amountInPaise;
    }

    public void setAmountInPaise(Long amountInPaise) {
        this.amountInPaise = amountInPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getPlatformFeeInPaise() {
        return platformFeeInPaise;
    }

    public void setPlatformFeeInPaise(Long platformFeeInPaise) {
        this.platformFeeInPaise = platformFeeInPaise;
    }

    public Long getMerchantNetInPaise() {
        return merchantNetInPaise;
    }

    public void setMerchantNetInPaise(Long merchantNetInPaise) {
        this.merchantNetInPaise = merchantNetInPaise;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public QrCode getQrCode() {
        return qrCode;
    }

    public void setQrCode(QrCode qrCode) {
        this.qrCode = qrCode;
    }

    public List<PaymentTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PaymentTransaction> transactions) {
        this.transactions = transactions;
    }

    public List<PaymentEvent> getEvents() {
        return events;
    }

    public void setEvents(List<PaymentEvent> events) {
        this.events = events;
    }

    public List<RefundOrder> getRefunds() {
        return refunds;
    }

    public void setRefunds(List<RefundOrder> refunds) {
        this.refunds = refunds;
    }

    // ==================== Helper Methods ====================

    /**
     * Check if this payment has expired
     * Returns true if: now > expiresAt AND status is not terminal
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        boolean isPastExpiry = now.isAfter(expiresAt);
        boolean isTerminal = status == PaymentStatus.SUCCESS || status == PaymentStatus.FAILED;
        return isPastExpiry && !isTerminal;
    }

    /**
     * Check if payment is in terminal state (no further changes possible)
     */
    public boolean isTerminal() {
        return status == PaymentStatus.SUCCESS ||
               status == PaymentStatus.FAILED ||
               status == PaymentStatus.EXPIRED ||
               status == PaymentStatus.CANCELLED;
    }

    /**
     * Calculate total refunded amount
     */
    public Long getTotalRefundedInPaise() {
        return refunds.stream()
                .mapToLong(r -> r.getRefundAmountInPaise())
                .sum();
    }

    /**
     * Convert amount from paise to rupees (for display)
     */
    public BigDecimal getAmountInRupees() {
        return BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "PaymentOrder{" +
                "id=" + id +
                ", paymentUuid='" + paymentUuid + '\'' +
                ", status=" + status +
                ", amountInPaise=" + amountInPaise +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                ", isExpired=" + isExpired() +
                '}';
    }
}
