package org.personal.repository;

import org.personal.entity.Payment;
import org.personal.entity.PaymentEvent;
import org.personal.enums.PaymentEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    @Query("SELECT e FROM PaymentEvent e WHERE e.payment = :payment ORDER BY e.createdAt ASC")
    List<PaymentEvent> findByPayment(@Param("payment") Payment payment);

    List<PaymentEvent> findByEventType(PaymentEventType eventType);

    @Query("SELECT e FROM PaymentEvent e WHERE e.payment = :payment AND e.eventType = :eventType")
    List<PaymentEvent> findByPaymentAndEventType(
        @Param("payment") Payment payment,
        @Param("eventType") PaymentEventType eventType
    );

    @Query("SELECT e FROM PaymentEvent e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentEvent> findByCreatedAtRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    long countByPayment(Payment payment);

    long countByEventType(PaymentEventType eventType);

    @Query("SELECT e FROM PaymentEvent e WHERE e.payment = :payment ORDER BY e.createdAt DESC LIMIT 1")
    PaymentEvent findLatestByPayment(@Param("payment") Payment payment);
}
