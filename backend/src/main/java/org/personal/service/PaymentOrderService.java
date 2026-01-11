package org.personal.service;

import org.personal.entity.*;
import org.personal.enums.PaymentStatus;
import org.personal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOrderService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final MerchantRepository merchantRepository;
    private final FeeCalculationService feeCalculationService;
    private final QrCodeService qrCodeService;

    public PaymentOrderService(PaymentOrderRepository paymentOrderRepository,
                               PaymentEventRepository paymentEventRepository,
                               MerchantRepository merchantRepository,
                               FeeCalculationService feeCalculationService,
                               QrCodeService qrCodeService) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.merchantRepository = merchantRepository;
        this.feeCalculationService = feeCalculationService;
        this.qrCodeService = qrCodeService;
    }

    public PaymentOrder createPaymentOrder(Merchant merchant, Long amountInPaise,
                                          String externalOrderId, String description,
                                          Integer expiryMinutes) {
        if (amountInPaise == null || amountInPaise <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        PaymentOrder payment = new PaymentOrder();
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

        PaymentOrder saved = paymentOrderRepository.save(payment);
        logger.info("Payment created: UUID={}", saved.getPaymentUuid());
        return saved;
    }

    public Optional<PaymentOrder> getPaymentOrder(String paymentUuid) {
        Optional<PaymentOrder> paymentOpt = paymentOrderRepository.findByPaymentUuid(paymentUuid);

        if (paymentOpt.isPresent()) {
            PaymentOrder payment = paymentOpt.get();
            if (payment.isExpired()) {
                logger.info("Payment {} expired", paymentUuid);
                payment.setStatus(PaymentStatus.EXPIRED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentOrderRepository.save(payment);
            }
        }

        return paymentOpt;
    }

    public PaymentOrder generateQrCode(String paymentUuid, String upiVpa, String upiName) {
        PaymentOrder payment = getPaymentOrder(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException("Cannot generate QR");
        }

        QrCode qrCode = qrCodeService.generateQrCode(payment, upiVpa, upiName);

        payment.setStatus(PaymentStatus.QR_GENERATED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setQrCode(qrCode);

        PaymentOrder updated = paymentOrderRepository.save(payment);
        return updated;
    }

    public PaymentOrder transitionToPending(String paymentUuid) {
        PaymentOrder payment = getPaymentOrder(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.QR_GENERATED) {
            throw new IllegalStateException("Invalid state");
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentOrder updated = paymentOrderRepository.save(payment);

        return updated;
    }

    public PaymentOrder markAsSuccess(String paymentUuid) {
        PaymentOrder payment = getPaymentOrder(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentOrder updated = paymentOrderRepository.save(payment);

        return updated;
    }

    public PaymentOrder markAsFailed(String paymentUuid, String reason) {
        PaymentOrder payment = getPaymentOrder(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentOrder updated = paymentOrderRepository.save(payment);

        return updated;
    }

    public PaymentOrder cancelPayment(String paymentUuid) {
        PaymentOrder payment = getPaymentOrder(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.isTerminal()) {
            throw new IllegalStateException("Cannot cancel");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentOrder updated = paymentOrderRepository.save(payment);

        return updated;
    }

    public List<PaymentEvent> getAuditTrail(String paymentUuid) {
        PaymentOrder payment = getPaymentOrder(paymentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        return paymentEventRepository.findByPaymentOrder(payment);
    }

    public List<PaymentOrder> getPaymentsByMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        return paymentOrderRepository.findByMerchant(merchant);
    }

    /**
     * Get payment order by internal ID
     * @param id Internal database ID
     * @return PaymentOrder if found
     */
    public Optional<PaymentOrder> getPaymentOrderById(Long id) {
        Optional<PaymentOrder> paymentOpt = paymentOrderRepository.findById(id);

        if (paymentOpt.isPresent()) {
            PaymentOrder payment = paymentOpt.get();
            if (payment.isExpired()) {
                logger.info("Payment {} expired", payment.getPaymentUuid());
                payment.setStatus(PaymentStatus.EXPIRED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentOrderRepository.save(payment);
            }
        }

        return paymentOpt;
    }
}
