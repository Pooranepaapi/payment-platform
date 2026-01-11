package org.personal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.personal.enums.MerchantStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MasterMerchant Entity
 *
 * Represents a top-level organization that manages one or more child merchants.
 * Example: "Payment Gateway Company Ltd" can manage multiple store merchants
 *
 * Relationships:
 * - One MasterMerchant → Many Merchant (children)
 * - One MasterMerchant → Many MasterMerchantContract (contracts)
 *
 * Audit Trail:
 * - createdAt: Immutable, set on creation
 * - updatedAt: Updated on any modification
 * - deletedAt: Soft delete marker (NULL = active, timestamp = deleted)
 */
@Entity
@Table(
    name = "master_merchant",
    indexes = {
        @Index(name = "idx_master_merchant_uuid", columnList = "master_merchant_uuid", unique = true),
        @Index(name = "idx_master_merchant_status", columnList = "status")
    }
)
@Getter
@Setter
public class MasterMerchant {

    // ==================== Primary Key ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Unique Identifier (External) ====================

    /**
     * UUID for external reference (APIs, webhooks, customer links)
     * - Unique globally
     * - Not sequential (better for security)
     * - 36 characters (with hyphens)
     */
    @Column(name = "master_merchant_uuid", nullable = false, unique = true, length = 36)
    private String masterMerchantUuid = UUID.randomUUID().toString();

    // ==================== Business Logic Fields ====================

    /**
     * Organization name
     * Example: "Payment Gateway Company Ltd"
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Organization status (ACTIVE, INACTIVE, SUSPENDED)
     * - ACTIVE: Can create payments
     * - INACTIVE: Temporarily disabled
     * - SUSPENDED: Disabled due to fraud/violation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    // ==================== Timestamps ====================

    /**
     * When MasterMerchant was created
     * - Immutable (updatable = false)
     * - Set automatically in @PrePersist
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last modified timestamp
     * - Updated whenever entity changes
     * - Set automatically in @PrePersist and @PreUpdate
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Soft delete marker
     * - NULL = active (normal state)
     * - timestamp = deleted at this time (soft deleted)
     * - Allows audit trail without hard deletion
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ==================== Relationships ====================

    /**
     * Child merchants under this MasterMerchant
     * - One-to-Many relationship
     * - cascade = NONE: Don't delete children when parent deleted
     * - fetch = LAZY: Don't load children unless explicitly accessed
     */
    @OneToMany(mappedBy = "masterMerchant", fetch = FetchType.LAZY)
    private List<Merchant> childMerchants = new ArrayList<>();

    // ==================== Lifecycle Callbacks ====================

    /**
     * Set timestamps before persisting to database
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (masterMerchantUuid == null) {
            masterMerchantUuid = UUID.randomUUID().toString();
        }
    }

    /**
     * Update timestamp before any update
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Helper Methods ====================

    /**
     * Check if this MasterMerchant is active (not soft-deleted and not suspended)
     */
    public boolean isActive() {
        return deletedAt == null && status == MerchantStatus.ACTIVE;
    }

    /**
     * Check if this MasterMerchant is soft-deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft delete this MasterMerchant
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restore a soft-deleted MasterMerchant
     */
    public void restore() {
        this.deletedAt = null;
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "MasterMerchant{" +
                "id=" + id +
                ", masterMerchantUuid='" + masterMerchantUuid + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", isDeleted=" + (deletedAt != null) +
                '}';
    }
}
