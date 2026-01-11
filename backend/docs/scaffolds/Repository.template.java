package org.personal.repository;

import org.personal.entity.[ENTITY_NAME];
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for [ENTITY_NAME]
 *
 * Usage:
 * 1. Copy this file: cp Repository.template.java [ENTITY_NAME]Repository.java
 * 2. Replace [ENTITY_NAME] with your entity name
 * 3. Replace [TABLE_NAME] with your table name
 * 4. Implement query methods using method names or @Query
 *
 * Method Naming Convention:
 * - findBy[Field]: SELECT * WHERE [field] = ?
 * - findAllBy[Field]: SELECT * WHERE [field] = ? (paginated)
 * - findBy[Field1]And[Field2]: SELECT * WHERE [field1] = ? AND [field2] = ?
 * - findBy[Field]OrderBy[SortField]: SELECT * WHERE [field] = ? ORDER BY [sortField]
 * - existsBy[Field]: Returns true/false
 * - countBy[Field]: Returns count
 * - deleteBy[Field]: DELETE WHERE [field] = ?
 *
 * Complex Queries:
 * - Use @Query for WHERE clauses with IN, LIKE, or multiple joins
 * - Use JPQL (not SQL) - more portable
 * - Use named parameters (:param) not positional (?)
 */
@Repository
public interface [ENTITY_NAME]Repository extends JpaRepository<[ENTITY_NAME], Long> {

    // ==================== Simple Queries (Method Names) ====================

    /**
     * Find by UUID (external reference)
     * Common pattern: External APIs use UUID, internal code uses Long ID
     */
    Optional<[ENTITY_NAME]> findBy[ENTITY_NAME]Uuid(String [table_name]Uuid);

    /**
     * Check if entity exists by UUID
     */
    boolean existsBy[ENTITY_NAME]Uuid(String [table_name]Uuid);

    // ==================== Filtered Queries (@Query) ====================

    /**
     * Find by merchant and status (uses composite index: merchant_id, status, created_at)
     * Paginated result (returns Page, not List)
     *
     * Index: idx_[table_name]_main(merchant_id, status, created_at)
     * Performance: Index range scan + sort
     */
    @Query("""
        SELECT e FROM [ENTITY_NAME] e
        WHERE e.merchant.id = :merchantId
        AND e.status = :status
        AND e.deletedAt IS NULL
        ORDER BY e.createdAt DESC
    """)
    Page<[ENTITY_NAME]> findByMerchantAndStatus(
        Long merchantId,
        [ENUM_STATUS] status,
        Pageable pageable
    );

    /**
     * Find expired entities (on-read expiry evaluation)
     * Called by background job or on-read check
     *
     * Index: idx_[table_name]_expires(expiresAt, status)
     * Performance: Index range scan
     */
    @Query("""
        SELECT e FROM [ENTITY_NAME] e
        WHERE e.expiresAt < :now
        AND e.status IN ('PENDING', 'QR_GENERATED')
        AND e.deletedAt IS NULL
    """)
    List<[ENTITY_NAME]> findExpiredEntities(Instant now);

    /**
     * Find active entities (not soft-deleted)
     */
    @Query("""
        SELECT e FROM [ENTITY_NAME] e
        WHERE e.deletedAt IS NULL
    """)
    List<[ENTITY_NAME]> findActive();

    /**
     * Find by entity with merchant details (JOIN FETCH to avoid N+1)
     * Used when you need both [ENTITY_NAME] and its Merchant in same query
     */
    @Query("""
        SELECT e FROM [ENTITY_NAME] e
        JOIN FETCH e.merchant m
        WHERE e.[table_name]Uuid = :uuid
        AND e.deletedAt IS NULL
    """)
    Optional<[ENTITY_NAME]> findBy[ENTITY_NAME]UuidWithMerchant(String uuid);

    /**
     * Count by status (for metrics/dashboards)
     */
    @Query("""
        SELECT COUNT(e) FROM [ENTITY_NAME] e
        WHERE e.status = :status
        AND e.deletedAt IS NULL
    """)
    Long countByStatus([ENUM_STATUS] status);

    // ==================== Batch Operations ====================

    /**
     * Batch update status (for state transitions)
     * Use case: Mark all expired payments as EXPIRED
     */
    @Query("""
        UPDATE [ENTITY_NAME] e
        SET e.status = :newStatus,
            e.updatedAt = :now
        WHERE e.[table_name]Uuid IN :uuids
        AND e.deletedAt IS NULL
    """)
    int updateStatusByUuids(List<String> uuids, [ENUM_STATUS] newStatus, Instant now);

    /**
     * Soft delete by UUID
     */
    @Query("""
        UPDATE [ENTITY_NAME] e
        SET e.deletedAt = :now
        WHERE e.[table_name]Uuid = :uuid
    """)
    int softDeleteByUuid(String uuid, Instant now);

    // ==================== Custom Queries (if needed) ====================

    /**
     * Example: Find entities by multiple criteria
     * Use @Query for complex WHERE clauses
     */
    @Query("""
        SELECT e FROM [ENTITY_NAME] e
        WHERE (:status IS NULL OR e.status = :status)
        AND (:createdAfter IS NULL OR e.createdAt > :createdAfter)
        AND e.deletedAt IS NULL
        ORDER BY e.createdAt DESC
    """)
    Page<[ENTITY_NAME]> searchWithFilters(
        [ENUM_STATUS] status,
        Instant createdAfter,
        Pageable pageable
    );

    // ==================== Native Queries (if absolutely necessary) ====================

    /**
     * Use native SQL only for queries that can't be expressed in JPQL
     * Example: Complex analytics, window functions, etc.
     *
     * nativeQuery = true: This is SQL, not JPQL
     * name = "[NamedQuery]": Optional, can be referenced elsewhere
     */
    @Query(
        value = """
            SELECT merchant_id, status, COUNT(*) as count
            FROM [TABLE_NAME]
            WHERE created_at > ?1
            GROUP BY merchant_id, status
        """,
        nativeQuery = true
    )
    List<Object[]> getStatusCountByMerchant(Instant createdAfter);
}
