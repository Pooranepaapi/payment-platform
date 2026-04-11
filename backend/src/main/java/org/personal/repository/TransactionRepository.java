package org.personal.repository;

import org.personal.entity.Payment;
import org.personal.entity.Transaction;
import org.personal.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionUuid(String transactionUuid);

    /** Find by PSP transaction ID — used for idempotency checks. */
    Optional<Transaction> findByPspTransactionId(String pspTransactionId);

    List<Transaction> findByPayment(Payment payment);

    List<Transaction> findByPaymentAndStatus(Payment payment, TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.payment = :payment ORDER BY t.createdAt DESC LIMIT 1")
    Optional<Transaction> findLatestByPayment(@Param("payment") Payment payment);

    List<Transaction> findByStatus(TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.payment = :payment " +
           "AND t.status = org.personal.enums.TransactionStatus.SUCCESS")
    Optional<Transaction> findSuccessfulTransaction(@Param("payment") Payment payment);

    List<Transaction> findByPspName(String pspName);

    @Query("SELECT t FROM Transaction t WHERE t.status = org.personal.enums.TransactionStatus.FAILED " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findFailedTransactionsInRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    boolean existsByTransactionUuid(String transactionUuid);

    boolean existsByPspTransactionId(String pspTransactionId);

    long countByPaymentAndStatus(Payment payment, TransactionStatus status);
}
