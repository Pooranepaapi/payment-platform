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
 * Payment Entity (Phase-1 QR Payment System)
 *
 * Represents a payment request with an associated QR code.
 * Immutable snapshot: fees, amount, and merchant details frozen at creation.
 *
 * State Machine:
 * CREATED → QR_GENERATED → PENDING → SUCCESS / FAILED / EXPIRED / CANCELLED
 */
@Entity
@Table(
    name = "payment",
    indexes = {
        @Index(name = "idx_payment_uuid", columnList = "payment_uuid", unique = true),
        @Index(name = "idx_payment_main", columnList = "merchant_id,status,created_at"),
        @Index(name = "idx_payment_expires", columnList = "expires_at,status"),
        @Index(name = "idx_payment_created", columnList = "created_at DESC")
    }
)
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_uuid", nullable = false, unique = true, length = 36)
    private String paymentUuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.CREATED;

    /** Amount in paise (₹100.50 = 10050 paise). */
    @Column(name = "amount_in_paise", nullable = false)
    private Long amountInPaise;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    /** Platform fee in paise — frozen at creation. */
    @Column(name = "platform_fee_in_paise", nullable = false)
    private Long platformFeeInPaise;

    /** Merchant net after fee in paise — frozen at creation. */
    @Column(name = "merchant_net_in_paise", nullable = false)
    private Long merchantNetInPaise;

    @Column(name = "external_order_id", length = 255)
    private String externalOrderId;

    @Column(name = "description", length = 255)
    private String description;

    /** QR code expires after 15 min; checked on every read. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY)
    private QrCode qrCode;

    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY)
    private List<PaymentEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY)
    private List<RefundOrder> refunds = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (paymentUuid == null) paymentUuid = UUID.randomUUID().toString();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        boolean isPastExpiry = now.isAfter(expiresAt);
        boolean isTerminal = status == PaymentStatus.SUCCESS || status == PaymentStatus.FAILED;
        return isPastExpiry && !isTerminal;
    }

    public boolean isTerminal() {
        return status == PaymentStatus.SUCCESS ||
               status == PaymentStatus.FAILED ||
               status == PaymentStatus.EXPIRED ||
               status == PaymentStatus.CANCELLED;
    }

    public Long getTotalRefundedInPaise() {
        return refunds.stream().mapToLong(r -> r.getRefundAmountInPaise()).sum();
    }

    public BigDecimal getAmountInRupees() {
        return BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "Payment{id=" + id + ", paymentUuid='" + paymentUuid + "', status=" + status +
               ", amountInPaise=" + amountInPaise + ", currency='" + currency + "', createdAt=" + createdAt + '}';
    }
}
