package org.personal.repository;

import org.personal.entity.MasterMerchant;
import org.personal.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MasterMerchant entity
 * Handles database operations for master merchant records
 */
@Repository
public interface MasterMerchantRepository extends JpaRepository<MasterMerchant, Long> {

    /**
     * Find MasterMerchant by UUID (external reference)
     * @param masterMerchantUuid Unique identifier
     * @return MasterMerchant if found
     */
    Optional<MasterMerchant> findByMasterMerchantUuid(String masterMerchantUuid);

    /**
     * Find all active MasterMerchants (not soft-deleted)
     * @return List of active master merchants
     */
    @Query("SELECT m FROM MasterMerchant m WHERE m.deletedAt IS NULL AND m.status = :status")
    List<MasterMerchant> findActiveByStatus(@Param("status") MerchantStatus status);

    /**
     * Find all active (non-deleted) MasterMerchants
     * @return List of active master merchants
     */
    @Query("SELECT m FROM MasterMerchant m WHERE m.deletedAt IS NULL")
    List<MasterMerchant> findAllActive();

    /**
     * Check if MasterMerchant UUID exists
     * @param masterMerchantUuid UUID to check
     * @return true if exists
     */
    boolean existsByMasterMerchantUuid(String masterMerchantUuid);

    /**
     * Find all MasterMerchants by status
     * @param status MerchantStatus
     * @return List of master merchants with given status
     */
    List<MasterMerchant> findByStatus(MerchantStatus status);
}
