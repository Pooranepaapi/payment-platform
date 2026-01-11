package org.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.personal.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentTransaction Entity
 *
 * Represents one PSP (Payment Service Provider) interaction attempt.
 * Tracks communication with the payment service for a single payment.
 *
 * Design Principle:
 * - One PaymentOrder can have multiple PaymentTransaction attempts
 * - Example: First attempt times out, second attempt succeeds
 * - Each transaction tracked separately for reconciliation
 * - PSP reference IDs used for idempotency (prevent duplicate charges)
 *
 * Relationship:
 * - Many-to-One: Many transactions belong to one PaymentOrder
 *
 * State Machine (Valid Transitions):
 * INITIATED
 *   └─→ PENDING (waiting for PSP response)
 *
 * PENDING
 *   └─→ SUCCESS (PSP approved)
 *   └─→ FAILED (PSP declined)
 *
 * SUCCESS / FAILED
 *   └─→ (TERMINAL - no further changes)
 *
 * Idempotency:
 * - pspTransactionId: Unique at PSP (idempotency key)
 * - If duplicate request with same pspTransactionId arrives, return cached result
 */
@Entity
@Table(
    name = "payment_transaction",
    indexes = {
        @Index(name = "idx_psp_txn_id", columnList = "psp_transaction_id", unique = true),
        @Index(name = "idx_transaction_payment", columnList = "payment_order_id"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_created", columnList = "created_at DESC")
    }
)
@Getter
@Setter
public class PaymentTransaction {

    // ==================== Primary Key ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== External References ====================

    /**
     * UUID for audit trail and webhooks
     * - Unique globally
     * - Used in webhook notifications
     * - For tracing this specific transaction
     */
    @Column(name = "transaction_uuid", nullable = false, unique = true, length = 36)
    private String transactionUuid = UUID.randomUUID().toString();

    // ==================== Payment Reference ====================

    /**
     * Which payment does this transaction belong to?
     * - Many-to-One relationship
     * - Foreign key: payment_order_id
     * - Never null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    // ==================== PSP References ====================

    /**
     * PSP's unique transaction ID (idempotency key)
     * - Unique at PSP level
     * - Used to prevent duplicate processing
     * - If we see same pspTransactionId twice, return cached result
     * - Example: "AXIS_TXN_123456789"
     * - UNIQUE constraint ensures no duplicates
     */
    @Column(name = "psp_transaction_id", nullable = false, unique = true, length = 100)
    private String pspTransactionId;

    /**
     * PSP's approval code (if transaction succeeded)
     * - Only present if status = SUCCESS
     * - Used for reconciliation with PSP
     * - Example: "ABC123XYZ789"
     */
    @Column(name = "psp_approval_code", length = 50)
    private String pspApprovalCode;

    // ==================== Transaction Status ====================

    /**
     * Current transaction status
     * Tracks communication state with PSP
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.INITIATED;

    /**
     * Failure reason (if transaction failed)
     * - Only present if status = FAILED
     * - What went wrong? (decline reason, timeout, invalid data, etc.)
     * - Example: "Card declined by issuer"
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Which PSP handled this transaction?
     * - Example: "AXIS", "HDFC", "ICICI", "Google Pay", "Paytm"
     * - Used for routing and reconciliation
     */
    @Column(name = "psp_name", nullable = false, length = 50)
    private String pspName;

    // ==================== Timestamps ====================

    /**
     * When transaction was initiated
     * - Immutable (updatable = false)
     * - When we sent the request to PSP
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last modified timestamp
     * - Updated when status changes (INITIATED → PENDING → SUCCESS/FAILED)
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
        if (transactionUuid == null) {
            transactionUuid = UUID.randomUUID().toString();
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

    public String getTransactionUuid() {
        return transactionUuid;
    }

    public void setTransactionUuid(String transactionUuid) {
        this.transactionUuid = transactionUuid;
    }

    public PaymentOrder getPaymentOrder() {
        return paymentOrder;
    }

    public void setPaymentOrder(PaymentOrder paymentOrder) {
        this.paymentOrder = paymentOrder;
    }

    public String getPspTransactionId() {
        return pspTransactionId;
    }

    public void setPspTransactionId(String pspTransactionId) {
        this.pspTransactionId = pspTransactionId;
    }

    public String getPspApprovalCode() {
        return pspApprovalCode;
    }

    public void setPspApprovalCode(String pspApprovalCode) {
        this.pspApprovalCode = pspApprovalCode;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getPspName() {
        return pspName;
    }

    public void setPspName(String pspName) {
        this.pspName = pspName;
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
     * Check if transaction is in terminal state (no further changes possible)
     */
    public boolean isTerminal() {
        return status == TransactionStatus.SUCCESS || status == TransactionStatus.FAILED;
    }

    /**
     * Check if transaction succeeded
     */
    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "id=" + id +
                ", transactionUuid='" + transactionUuid + '\'' +
                ", pspTransactionId='" + pspTransactionId + '\'' +
                ", status=" + status +
                ", pspName='" + pspName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
