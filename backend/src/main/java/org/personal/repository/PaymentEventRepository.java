package org.personal.repository;

import org.personal.entity.PaymentEvent;
import org.personal.entity.PaymentOrder;
import org.personal.enums.PaymentEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for PaymentEvent entity
 * Handles database operations for payment event audit trail (append-only)
 */
@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    /**
     * Find all events for a payment (audit trail)
     * Returns in creation order (oldest first)
     * @param paymentOrder PaymentOrder entity
     * @return List of events for this payment
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.paymentOrder = :paymentOrder ORDER BY e.createdAt ASC")
    List<PaymentEvent> findByPaymentOrder(@Param("paymentOrder") PaymentOrder paymentOrder);

    /**
     * Find all events of a specific type
     * Used for analytics: count how many PENDING events, SUCCESS events, etc.
     * @param eventType PaymentEventType enum
     * @return List of events of that type
     */
    List<PaymentEvent> findByEventType(PaymentEventType eventType);

    /**
     * Find all events for a payment of a specific type
     * Example: Find all SUCCESS events for a payment
     * @param paymentOrder PaymentOrder entity
     * @param eventType PaymentEventType enum
     * @return List of events matching both criteria
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.paymentOrder = :paymentOrder AND e.eventType = :eventType")
    List<PaymentEvent> findByPaymentOrderAndEventType(
        @Param("paymentOrder") PaymentOrder paymentOrder,
        @Param("eventType") PaymentEventType eventType
    );

    /**
     * Find all events created within a time range
     * Used for daily reports, analytics, time-based queries
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return List of events in time range
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentEvent> findByCreatedAtRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count total events for a payment
     * Useful for analytics: how many state transitions?
     * @param paymentOrder PaymentOrder entity
     * @return Count of events
     */
    long countByPaymentOrder(PaymentOrder paymentOrder);

    /**
     * Count events by type
     * Useful for analytics: how many SUCCESS, FAILED, EXPIRED events?
     * @param eventType PaymentEventType enum
     * @return Count of events of that type
     */
    long countByEventType(PaymentEventType eventType);

    /**
     * Find the most recent event for a payment
     * Used to determine current state
     * @param paymentOrder PaymentOrder entity
     * @return Latest PaymentEvent
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.paymentOrder = :paymentOrder ORDER BY e.createdAt DESC LIMIT 1")
    PaymentEvent findLatestByPaymentOrder(@Param("paymentOrder") PaymentOrder paymentOrder);
}
