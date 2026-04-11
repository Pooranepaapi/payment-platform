package org.personal.repository;

import org.personal.entity.Payment;
import org.personal.entity.RefundOrder;
import org.personal.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundOrderRepository extends JpaRepository<RefundOrder, Long> {

    Optional<RefundOrder> findByRefundUuid(String refundUuid);

    List<RefundOrder> findByPayment(Payment payment);

    List<RefundOrder> findByPaymentAndStatus(Payment payment, RefundStatus status);

    @Query("SELECT r FROM RefundOrder r WHERE r.payment = :payment AND r.status = org.personal.enums.RefundStatus.SUCCESS")
    List<RefundOrder> findSuccessfulRefunds(@Param("payment") Payment payment);

    @Query("SELECT SUM(r.refundAmountInPaise) FROM RefundOrder r WHERE r.payment = :payment AND r.status = org.personal.enums.RefundStatus.SUCCESS")
    Long sumSuccessfulRefundAmount(@Param("payment") Payment payment);

    List<RefundOrder> findByStatus(RefundStatus status);

    @Query("SELECT r FROM RefundOrder r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<RefundOrder> findByCreatedAtRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT r FROM RefundOrder r WHERE r.payment = :payment ORDER BY r.createdAt DESC LIMIT 1")
    Optional<RefundOrder> findLatestByPayment(@Param("payment") Payment payment);

    boolean existsByRefundUuid(String refundUuid);

    long countByPaymentAndStatus(Payment payment, RefundStatus status);
}
