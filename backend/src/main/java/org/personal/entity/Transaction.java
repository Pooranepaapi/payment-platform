package org.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.personal.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Entity
 *
 * Represents one PSP interaction attempt for a Payment.
 * One Payment can have multiple Transaction attempts (retry on failure).
 *
 * State Machine: INITIATED → PENDING → SUCCESS / FAILED
 *
 * Idempotency: pspTransactionId has a UNIQUE constraint to prevent duplicate processing.
 */
@Entity
@Table(
    name = "transaction",
    indexes = {
        @Index(name = "idx_psp_txn_id", columnList = "psp_transaction_id", unique = true),
        @Index(name = "idx_transaction_payment", columnList = "payment_id"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_created", columnList = "created_at DESC")
    }
)
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_uuid", nullable = false, unique = true, length = 36)
    private String transactionUuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /** PSP's unique transaction ID — idempotency key. */
    @Column(name = "psp_transaction_id", nullable = false, unique = true, length = 100)
    private String pspTransactionId;

    /** PSP's approval code, present only on SUCCESS. */
    @Column(name = "psp_approval_code", length = 50)
    private String pspApprovalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Bank's own reference ID, received in the async callback. */
    @Column(name = "bank_reference_id", length = 100)
    private String bankReferenceId;

    /** Which PSP handled this transaction (e.g. "SIMULATOR", "AXIS"). */
    @Column(name = "psp_name", nullable = false, length = 50)
    private String pspName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (transactionUuid == null) transactionUuid = UUID.randomUUID().toString();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isTerminal() {
        return status == TransactionStatus.SUCCESS || status == TransactionStatus.FAILED;
    }

    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", transactionUuid='" + transactionUuid +
               "', pspTransactionId='" + pspTransactionId + "', status=" + status +
               ", pspName='" + pspName + "', createdAt=" + createdAt + '}';
    }
}
