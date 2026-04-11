package org.personal.repository;

import org.personal.entity.Merchant;
import org.personal.entity.Payment;
import org.personal.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentUuid(String paymentUuid);

    List<Payment> findByMerchantAndStatus(Merchant merchant, PaymentStatus status);

    List<Payment> findByMerchant(Merchant merchant);

    List<Payment> findByStatus(PaymentStatus status);

    /** Find payments past their expiry date that are not in a terminal state. */
    @Query("SELECT p FROM Payment p WHERE p.expiresAt < :now AND " +
           "p.status NOT IN (org.personal.enums.PaymentStatus.SUCCESS, " +
           "org.personal.enums.PaymentStatus.FAILED, " +
           "org.personal.enums.PaymentStatus.EXPIRED, " +
           "org.personal.enums.PaymentStatus.CANCELLED)")
    List<Payment> findExpiredPayments(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Payment p WHERE p.merchant = :merchant AND " +
           "p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByMerchantAndDateRange(
        @Param("merchant") Merchant merchant,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    boolean existsByPaymentUuid(String paymentUuid);

    long countByMerchantAndStatus(Merchant merchant, PaymentStatus status);
}
