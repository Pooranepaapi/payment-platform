package org.personal.repository;

import org.personal.entity.Merchant;
import org.personal.entity.MasterMerchant;
import org.personal.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Merchant entity
 * Handles database operations for merchant records
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    /**
     * Find Merchant by UUID (external reference for QR payment system)
     * @param merchantUuid Unique identifier
     * @return Merchant if found
     */
    Optional<Merchant> findByMerchantUuid(String merchantUuid);

    /**
     * Find Merchant by VPA (UPI Virtual Payment Address)
     * Example: "storea@axis", "merchant@hdfc"
     * @param vpa UPI address
     * @return Merchant if found
     */
    Optional<Merchant> findByVpa(String vpa);

    /**
     * Find Merchant by legacy merchant_id (backward compatibility)
     * @param merchantId Legacy merchant ID
     * @return Merchant if found
     */
    Optional<Merchant> findByMerchantId(String merchantId);

    /**
     * Find all merchants under a MasterMerchant (parent)
     * @param masterMerchant Parent master merchant
     * @return List of child merchants
     */
    List<Merchant> findByMasterMerchant(MasterMerchant masterMerchant);

    /**
     * Find all active merchants under a MasterMerchant
     * @param masterMerchant Parent master merchant
     * @param status MerchantStatus (typically ACTIVE)
     * @return List of active child merchants
     */
    List<Merchant> findByMasterMerchantAndStatus(MasterMerchant masterMerchant, MerchantStatus status);

    /**
     * Find all merchants with given status
     * @param status MerchantStatus
     * @return List of merchants with that status
     */
    List<Merchant> findByStatus(MerchantStatus status);

    /**
     * Check if merchant UUID exists
     * @param merchantUuid UUID to check
     * @return true if exists
     */
    boolean existsByMerchantUuid(String merchantUuid);

    /**
     * Check if VPA is already registered
     * @param vpa UPI address to check
     * @return true if VPA exists
     */
    boolean existsByVpa(String vpa);

    /**
     * Find all active merchants (for QR payment system)
     * @return List of active merchants
     */
    @Query("SELECT m FROM Merchant m WHERE m.status = :status")
    List<Merchant> findAllActive(@Param("status") MerchantStatus status);

    /**
     * Check if merchant with legacy merchant_id exists
     * @param merchantId Legacy merchant ID to check
     * @return true if exists
     */
    boolean existsByMerchantId(String merchantId);
}
