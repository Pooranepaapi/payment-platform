package org.personal.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * [ENTITY_DESCRIPTION]
 *
 * Example: PaymentOrder represents a payment request with QR code.
 *
 * Usage:
 * 1. Copy this file: cp Entity.template.java MyEntity.java
 * 2. Replace [ENTITY_NAME] with your entity name
 * 3. Replace [TABLE_NAME] with your table name
 * 4. Add @Column fields for your business logic
 * 5. Add @ManyToOne, @OneToMany relationships as needed
 * 6. Generate getters/setters (IDE: Ctrl+Shift+P in VS Code, Cmd+Shift+P in Mac)
 */
@Entity
@Table(name = "[TABLE_NAME]", indexes = {
    @Index(name = "idx_[table_name]_uuid", columnList = "[table_name]_uuid"),
    @Index(name = "idx_[table_name]_created", columnList = "created_at DESC")
})
public class [ENTITY_NAME] {

    // ==================== Primary Key ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Unique Identifier (External) ====================

    /**
     * UUID for external reference (API, webhooks, customer links)
     * - Unique globally
     * - Not sequential (better for security)
     * - 36 characters (with hyphens)
     */
    @Column(unique = true, nullable = false, length = 36)
    private String [table_name]Uuid = UUID.randomUUID().toString();

    // ==================== Foreign Keys (Relationships) ====================

    /**
     * Example: @ManyToOne for "belongs to" relationship
     * - fetch = LAZY: Don't load merchant unless accessed
     * - nullable = false: This entity always has a merchant
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    // Alternative: @OneToMany for "has many" relationship
    // @OneToMany(mappedBy = "fieldNameInChild", cascade = CascadeType.NONE)
    // private List<ChildEntity> children = new ArrayList<>();

    // ==================== Enum Fields ====================

    /**
     * Always use @Enumerated(EnumType.STRING)
     * - Readable in database ("ACTIVE" not "0")
     * - Easier to add new enum values later
     * - Never use ORDINAL (breaks if enum order changes)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private [ENUM_NAME] status; // Example: PaymentStatus

    // ==================== Business Logic Fields ====================

    /**
     * For monetary amounts, always use BIGINT (paise, not rupees)
     * - ₹100.50 = 10050 paise
     * - No floating-point rounding errors
     * - All amounts are whole numbers
     */
    @Column(nullable = false)
    private Long amountInPaise;

    /**
     * Immutable snapshot at creation time
     * - calculated once, never changes
     * - Example: Platform fee calculated when payment created
     */
    @Column(nullable = false)
    private Long platformFeeInPaise;

    @Column(nullable = false)
    private String currency; // "INR", "USD", etc.

    /**
     * Optional fields (can be NULL)
     */
    @Column(nullable = true, length = 255)
    private String description;

    // ==================== Timestamps ====================

    /**
     * Use java.time.Instant (UTC, immutable)
     * - Not LocalDateTime (timezone-aware, mutable)
     * - Not java.util.Date (legacy)
     * - updatable = false: Never changes after creation
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Updated every time entity changes
     * - updatable = true (implicit)
     */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Soft delete: Mark as deleted without removing from DB
     * - NULL = active
     * - timestamp = deleted at this time
     * - Keeps audit trail intact
     */
    @Column(nullable = true)
    private Instant deletedAt;

    // ==================== Getters & Setters ====================
    // IDE Shortcut: Ctrl+Shift+P (VS Code) / Cmd+Shift+P (Mac)
    // Search: "Source Action" → "Generate Getters and Setters"

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String get[ENTITY_NAME]Uuid() {
        return [table_name]Uuid;
    }

    public void set[ENTITY_NAME]Uuid(String [table_name]Uuid) {
        this.[table_name]Uuid = [table_name]Uuid;
    }

    // ... (generate rest with IDE)

    // ==================== Helper Methods ====================

    /**
     * Check if this entity has expired
     * Used to implement on-read expiry evaluation
     */
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    /**
     * Check if this entity is soft-deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // ==================== toString (for logging) ====================

    @Override
    public String toString() {
        return "[ENTITY_NAME]{" +
                "id=" + id +
                ", [table_name]Uuid='" + [table_name]Uuid + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
