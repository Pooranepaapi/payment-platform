package org.personal.repository;

import org.personal.entity.Payment;
import org.personal.entity.Transaction;
import org.personal.enums.TransactionStatus;
import org.personal.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findByPayment(Payment payment);
    List<Transaction> findByPaymentAndTxnType(Payment payment, TransactionType txnType);
    List<Transaction> findByPaymentAndStatus(Payment payment, TransactionStatus status);
    boolean existsByTransactionId(String transactionId);
}
