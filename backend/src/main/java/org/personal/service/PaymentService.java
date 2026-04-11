package org.personal.service;

import org.personal.entity.*;
import org.personal.enums.PaymentStatus;
import org.personal.enums.PaymentType;
import org.personal.enums.TransactionStatus;
import org.personal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final MerchantRepository merchantRepository;
    private final FeeCalculationService feeCalculationService;
    private final QrCodeService qrCodeService;
    private final TransactionRepository transactionRepository;
    private final SimulatorClient simulatorClient;

    @Value("${app.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentEventRepository paymentEventRepository,
                          MerchantRepository merchantRepository,
                          FeeCalculationService feeCalculationService,
                          QrCodeService qrCodeService,
                          TransactionRepository transactionRepository,
                          SimulatorClient simulatorClient) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.merchantRepository = merchantRepository;
        this.feeCalculationService = feeCalculationService;
        this.qrCodeService = qrCodeService;
        this.transactionRepository = transactionRepository;
        this.simulatorClient = simulatorClient;
    }

    public Payment createPayment(Merchant merchant, Long amountInPaise,
                                 String externalOrderId, String description,
                                 Integer expiryMinutes) {
        if (amountInPaise == null || amountInPaise <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        Payment payment = new Payment();
        payment.setMerchant(merchant);
        payment.setAmountInPaise(amountInPaise);
        payment.setCurrency("INR");
        payment.setExternalOrderId(externalOrderId);
        payment.setDescription(description);
        payment.setStatus(PaymentStatus.CREATED);

        Long platformFee = feeCalculationService.calculatePlatformFee(merchant, amountInPaise);
        Long merchantNet = feeCalculationService.calculateMerchantNet(amountInPaise, platformFee);

        payment.setPlatformFeeInPaise(platformFee);
        payment.setMerchantNetInPaise(merchantNet);

        LocalDateTime now = LocalDateTime.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setExpiresAt(now.plusMinutes(expiryMinutes != null ? expiryMinutes : 15));

        Payment saved = paymentRepository.save(payment);
        logger.info("Payment created: UUID={}", saved.getPaymentUuid());
        return saved;
    }

    public Optional<Payment> getPayment(String paymentUuid) {
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentUuid(paymentUuid);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (payment.isExpired()) {
                logger.info("Payment {} expired", paymentUuid);
                payment.setStatus(PaymentStatus.EXPIRED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }

        return paymentOpt;
    }

    public Optional<Payment> getPaymentById(Long id) {
        Optional<Payment> paymentOpt = paymentRepository.findById(id);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (payment.isExpired()) {
                logger.info("Payment {} expired", payment.getPaymentUuid());
                payment.setStatus(PaymentStatus.EXPIRED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }

        return paymentOpt;
    }

    public Payment generateQrCode(String paymentUuid, String upiVpa, String upiName) {
        Payment payment = getPayment(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException("Cannot generate QR");
        }

        QrCode qrCode = qrCodeService.generateQrCode(payment, upiVpa, upiName);

        payment.setStatus(PaymentStatus.QR_GENERATED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setQrCode(qrCode);

        return paymentRepository.save(payment);
    }

    public Payment transitionToPending(String paymentUuid) {
        Payment payment = getPayment(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.QR_GENERATED) {
            throw new IllegalStateException("Invalid state");
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment markAsSuccess(String paymentUuid) {
        Payment payment = getPayment(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment markAsFailed(String paymentUuid, String reason) {
        Payment payment = getPayment(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment cancelPayment(String paymentUuid) {
        Payment payment = getPayment(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.isTerminal()) {
            throw new IllegalStateException("Cannot cancel");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public List<PaymentEvent> getAuditTrail(String paymentUuid) {
        Payment payment = getPayment(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        return paymentEventRepository.findByPayment(payment);
    }

    public List<Payment> getPaymentsByMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        return paymentRepository.findByMerchant(merchant);
    }

    public Transaction initiateUpiCollect(Long paymentId, String customerVpa) {
        Payment payment = getPaymentById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        Merchant merchant = payment.getMerchant();
        String merchantVpa = merchant.getVpa();
        if (merchantVpa == null || merchantVpa.isEmpty()) {
            merchantVpa = merchant.getMerchantId() + "@test";
        }

        String pspTransactionId = "UPI_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Transaction txn = new Transaction();
        txn.setPayment(payment);
        txn.setPspName("SIMULATOR");
        txn.setPspTransactionId(pspTransactionId);
        txn.setStatus(TransactionStatus.INITIATED);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setUpdatedAt(LocalDateTime.now());
        txn = transactionRepository.save(txn);

        BigDecimal amountInRupees = BigDecimal.valueOf(payment.getAmountInPaise()).divide(BigDecimal.valueOf(100));
        String callbackUrl = backendBaseUrl + "/api/v1/payments/upi/callback";

        try {
            SimulatorClient.SimulatorResponse simResponse = simulatorClient.initiateUpiCollect(
                    pspTransactionId, amountInRupees, customerVpa, merchantVpa, PaymentType.DYNAMIC_QR, callbackUrl);

            txn.setStatus(TransactionStatus.PENDING);
            txn.setUpdatedAt(LocalDateTime.now());
            txn = transactionRepository.save(txn);

            payment.setStatus(PaymentStatus.PENDING);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            logger.info("UPI collect initiated: paymentId={}, txnId={}, pspRef={}",
                    paymentId, txn.getId(), simResponse.getPspReferenceId());
        } catch (Exception e) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setFailureReason("Simulator unavailable: " + e.getMessage());
            txn.setUpdatedAt(LocalDateTime.now());
            txn = transactionRepository.save(txn);
            logger.error("UPI collect failed for paymentId={}: {}", paymentId, e.getMessage());
        }

        return txn;
    }

    public void processUpiCallback(String transactionId, String status, String bankReferenceId, String failureReason) {
        Transaction txn = transactionRepository.findByPspTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        Payment payment = txn.getPayment();

        if ("SUCCESS".equalsIgnoreCase(status)) {
            txn.setStatus(TransactionStatus.SUCCESS);
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setFailureReason(failureReason);
            payment.setStatus(PaymentStatus.FAILED);
        }

        LocalDateTime now = LocalDateTime.now();
        txn.setUpdatedAt(now);
        payment.setUpdatedAt(now);

        transactionRepository.save(txn);
        paymentRepository.save(payment);

        logger.info("UPI callback processed: transactionId={}, status={}", transactionId, status);
    }
}
