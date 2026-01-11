package org.personal.repository;

import org.personal.entity.QrCode;
import org.personal.entity.PaymentOrder;
import org.personal.enums.QrType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for QrCode entity
 * Handles database operations for QR codes (1:1 with PaymentOrder)
 */
@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    /**
     * Find QR code for a payment order (1:1 relationship)
     * @param paymentOrder PaymentOrder entity
     * @return QrCode if found
     */
    Optional<QrCode> findByPaymentOrder(PaymentOrder paymentOrder);

    /**
     * Find all expired QR codes (past expiresAt)
     * Used for cleanup, analytics, or retry logic
     * @param now Current timestamp
     * @return List of expired QR codes
     */
    @Query("SELECT q FROM QrCode q WHERE q.expiresAt < :now")
    List<QrCode> findExpiredQrCodes(@Param("now") LocalDateTime now);

    /**
     * Find all QR codes of a specific type
     * @param qrType QrType enum
     * @return List of QR codes of that type
     */
    List<QrCode> findByQrType(QrType qrType);

    /**
     * Find all QR codes created within a time range
     * Used for analytics and reporting
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return List of QR codes
     */
    @Query("SELECT q FROM QrCode q WHERE q.createdAt BETWEEN :startDate AND :endDate")
    List<QrCode> findByCreatedAtRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find QR codes expiring within a time range
     * Used for expiry warnings or notifications
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return List of QR codes expiring in range
     */
    @Query("SELECT q FROM QrCode q WHERE q.expiresAt BETWEEN :startDate AND :endDate")
    List<QrCode> findByExpiryRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count total QR codes of a type
     * @param qrType QrType enum
     * @return Count of QR codes
     */
    long countByQrType(QrType qrType);
}
