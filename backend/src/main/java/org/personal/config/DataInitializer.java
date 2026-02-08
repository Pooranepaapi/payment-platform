package org.personal.config;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.personal.entity.Contract;
import org.personal.entity.Merchant;
import org.personal.enums.ContractStatus;
import org.personal.enums.MerchantStatus;
import org.personal.enums.PaymentMethod;
import org.personal.enums.PaymentType;
import org.personal.repository.ContractRepository;
import org.personal.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MerchantRepository merchantRepository;
    private final ContractRepository contractRepository;
    private final ObjectMapper objectMapper;

    public DataInitializer(MerchantRepository merchantRepository,
                           ContractRepository contractRepository,
                           ObjectMapper objectMapper) {
        this.merchantRepository = merchantRepository;
        this.contractRepository = contractRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing test data...");

        // Create test merchant if not exists
        if (!merchantRepository.existsByMerchantId("MER001")) {
            createTestMerchant();
        }

        log.info("Test data initialization complete.");
        printTestInfo();
    }

    private void createTestMerchant() {
        // Create merchant
        Merchant merchant = new Merchant();
        merchant.setMerchantId("MER001");
        merchant.setLegalName("Test Merchant Pvt Ltd");
        merchant.setDisplayName("Test Store");
        merchant.setUsername("testmerchant");
        merchant.setPasswordHash("$2a$10$placeholder"); // Not used in Phase 1
        merchant.setEmail("merchant@test.com");
        merchant.setMobile("9876543210");
        merchant.setStatus(MerchantStatus.ACTIVE);

        merchant = merchantRepository.save(merchant);
        log.info("Created test merchant: {}", merchant.getMerchantId());

        // Create UPI contract for merchant
        Contract upiContract = new Contract();
        upiContract.setContractId("CON001");
        upiContract.setMerchant(merchant);
        upiContract.setPaymentMethod(PaymentMethod.UPI);
        upiContract.setPaymentType(PaymentType.RBLUPI);
        upiContract.setCurrency("INR");
        upiContract.setStatus(ContractStatus.ACTIVE);

        try {
            Map<String, Object> params = Map.of(
                "merchantVpa", "teststore@rbl",
                "mcc", "5411",
                "merchantName", "Test Store"
            );
            upiContract.setParams(objectMapper.writeValueAsString(params));
        } catch (JacksonException e) {
            log.warn("Failed to serialize contract params", e);
        }

        contractRepository.save(upiContract);
        log.info("Created UPI contract: {}", upiContract.getContractId());

        // Create second merchant
        Merchant merchant2 = new Merchant();
        merchant2.setMerchantId("MER002");
        merchant2.setLegalName("Demo Shop Ltd");
        merchant2.setDisplayName("Demo Shop");
        merchant2.setUsername("demoshop");
        merchant2.setPasswordHash("$2a$10$placeholder");
        merchant2.setEmail("demo@shop.com");
        merchant2.setMobile("9876543211");
        merchant2.setStatus(MerchantStatus.ACTIVE);

        merchant2 = merchantRepository.save(merchant2);
        log.info("Created test merchant: {}", merchant2.getMerchantId());

        // Create UPI contract for second merchant
        Contract upiContract2 = new Contract();
        upiContract2.setContractId("CON002");
        upiContract2.setMerchant(merchant2);
        upiContract2.setPaymentMethod(PaymentMethod.UPI);
        upiContract2.setPaymentType(PaymentType.HDFCUPI);
        upiContract2.setCurrency("INR");
        upiContract2.setStatus(ContractStatus.ACTIVE);

        try {
            Map<String, Object> params = Map.of(
                "merchantVpa", "demoshop@hdfc",
                "mcc", "5411",
                "merchantName", "Demo Shop"
            );
            upiContract2.setParams(objectMapper.writeValueAsString(params));
        } catch (JacksonException e) {
            log.warn("Failed to serialize contract params", e);
        }

        contractRepository.save(upiContract2);
        log.info("Created UPI contract: {}", upiContract2.getContractId());
    }

    private void printTestInfo() {
        log.info("");
        log.info("=================================================");
        log.info("   PAYMENT GATEWAY - UPI POC (Phase 1)");
        log.info("=================================================");
        log.info("");
        log.info("Test Merchants:");
        log.info("  - MER001: Test Store (teststore@rbl)");
        log.info("  - MER002: Demo Shop (demoshop@hdfc)");
        log.info("");
        log.info("Test VPAs for UPI Simulator:");
        log.info("  - success@upi   : Always succeeds");
        log.info("  - fail@upi      : Always fails");
        log.info("  - timeout@upi   : Stays pending");
        log.info("  - insufficient@upi : Insufficient funds");
        log.info("");
        log.info("API Endpoints:");
        log.info("  POST /api/payments           - Create payment");
        log.info("  GET  /api/payments/{id}      - Get payment");
        log.info("  POST /api/payments/upi/collect - Initiate UPI collect");
        log.info("  POST /api/payments/refund    - Initiate refund");
        log.info("  GET  /api/transactions/{id}  - Get transaction");
        log.info("  POST /api/transactions/{id}/simulate-approval");
        log.info("                               - Simulate customer approval");
        log.info("");
        log.info("=================================================");
    }
}
