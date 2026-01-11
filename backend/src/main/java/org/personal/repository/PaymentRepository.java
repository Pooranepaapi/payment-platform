package org.personal.repository;

import org.personal.entity.Merchant;
import org.personal.entity.Payment;
import org.personal.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    List<Payment> findByMerchant(Merchant merchant);
    List<Payment> findByMerchantAndStatus(Merchant merchant, PaymentStatus status);
    Optional<Payment> findByMerchantOrderIdAndMerchant(String merchantOrderId, Merchant merchant);
    boolean existsByPaymentId(String paymentId);
}
