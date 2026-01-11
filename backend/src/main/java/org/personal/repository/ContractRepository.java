package org.personal.repository;

import org.personal.entity.Contract;
import org.personal.entity.Merchant;
import org.personal.enums.ContractStatus;
import org.personal.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByContractId(String contractId);
    List<Contract> findByMerchant(Merchant merchant);
    List<Contract> findByMerchantAndStatus(Merchant merchant, ContractStatus status);
    List<Contract> findByMerchantAndPaymentMethodAndStatus(Merchant merchant, PaymentMethod paymentMethod, ContractStatus status);
    boolean existsByContractId(String contractId);
}
