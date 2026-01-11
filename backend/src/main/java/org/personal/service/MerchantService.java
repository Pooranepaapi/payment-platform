package org.personal.service;

import org.personal.entity.Merchant;
import org.personal.entity.MasterMerchant;
import org.personal.enums.MerchantStatus;
import org.personal.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MerchantService
 * Manages merchant entities (stores, shops)
 * 
 * Responsibilities:
 * - Create/update merchant records
 * - Manage merchant status (ACTIVE, INACTIVE, SUSPENDED)
 * - Validate merchant data
 * - Support master-child merchant hierarchy
 */
@Service
@Transactional
public class MerchantService {

    private static final Logger logger = LoggerFactory.getLogger(MerchantService.class);
    
    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    /**
     * Find merchant by UUID
     * @param merchantUuid Merchant UUID
     * @return Merchant if found
     */
    public Optional<Merchant> findByUuid(String merchantUuid) {
        return merchantRepository.findByMerchantUuid(merchantUuid);
    }

    /**
     * Find merchant by VPA (UPI address)
     * @param vpa UPI virtual payment address
     * @return Merchant if found
     */
    public Optional<Merchant> findByVpa(String vpa) {
        return merchantRepository.findByVpa(vpa);
    }

    /**
     * Find merchant by legacy merchant ID
     * @param merchantId Legacy merchant ID
     * @return Merchant if found
     */
    public Optional<Merchant> findByMerchantId(String merchantId) {
        return merchantRepository.findByMerchantId(merchantId);
    }

    /**
     * Get all merchants under a MasterMerchant
     * @param masterMerchant Parent master merchant
     * @return List of child merchants
     */
    public List<Merchant> getChildMerchants(MasterMerchant masterMerchant) {
        return merchantRepository.findByMasterMerchant(masterMerchant);
    }

    /**
     * Get all active merchants
     * @return List of active merchants
     */
    public List<Merchant> getAllActive() {
        return merchantRepository.findAllActive(MerchantStatus.ACTIVE);
    }

    /**
     * Check if merchant UUID is available
     * @param merchantUuid UUID to check
     * @return true if available (not taken)
     */
    public boolean isUuidAvailable(String merchantUuid) {
        return !merchantRepository.existsByMerchantUuid(merchantUuid);
    }

    /**
     * Check if VPA is available
     * @param vpa UPI address to check
     * @return true if available (not taken)
     */
    public boolean isVpaAvailable(String vpa) {
        return !merchantRepository.existsByVpa(vpa);
    }

    /**
     * Activate a merchant
     * @param merchantId Merchant ID
     */
    public void activateMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        merchant.setStatus(MerchantStatus.ACTIVE);
        merchantRepository.save(merchant);
        logger.info("Merchant activated: {}", merchantId);
    }

    /**
     * Suspend a merchant (fraud/violation)
     * @param merchantId Merchant ID
     */
    public void suspendMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        merchant.setStatus(MerchantStatus.SUSPENDED);
        merchantRepository.save(merchant);
        logger.info("Merchant suspended: {}", merchantId);
    }

    /**
     * Check if merchant is active
     * @param merchantId Merchant ID
     * @return true if active
     */
    public boolean isActive(String merchantId) {
        return merchantRepository.findByMerchantId(merchantId)
                .map(m -> m.getStatus() == MerchantStatus.ACTIVE)
                .orElse(false);
    }
}
