package org.personal.service;

import org.personal.entity.Merchant;
import org.springframework.stereotype.Service;

/**
 * FeeCalculationService
 * Calculates platform fees based on merchant fee percentage
 * 
 * Business Logic:
 * - Each merchant has platformFeePercentage
 * - Fee = amount * (fee% / 100)
 * - Frozen at payment creation time
 */
@Service
public class FeeCalculationService {

    /**
     * Calculate platform fee for a payment
     * @param merchant Merchant entity
     * @param amountInPaise Amount in paise
     * @return Fee amount in paise
     */
    public Long calculatePlatformFee(Merchant merchant, Long amountInPaise) {
        if (merchant.getPlatformFeePercentage() == null) {
            merchant.setPlatformFeePercentage(java.math.BigDecimal.valueOf(2.5));
        }

        java.math.BigDecimal feePercentage = merchant.getPlatformFeePercentage();
        java.math.BigDecimal feeAmount = java.math.BigDecimal.valueOf(amountInPaise)
                .multiply(feePercentage)
                .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);

        return feeAmount.longValue();
    }

    /**
     * Calculate merchant net amount (after fees)
     * @param amountInPaise Amount in paise
     * @param platformFeeInPaise Platform fee in paise
     * @return Merchant net amount in paise
     */
    public Long calculateMerchantNet(Long amountInPaise, Long platformFeeInPaise) {
        return amountInPaise - platformFeeInPaise;
    }
}
