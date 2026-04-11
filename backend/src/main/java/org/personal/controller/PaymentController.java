package org.personal.controller;

import jakarta.validation.Valid;
import org.personal.dto.*;
import org.personal.entity.Merchant;
import org.personal.entity.Payment;
import org.personal.entity.Transaction;
import org.personal.entity.QrCode;
import org.personal.enums.PaymentStatus;
import org.personal.repository.MerchantRepository;
import org.personal.service.PaymentService;
import org.personal.service.QrCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Payment Controller
 * Handles payment flow endpoints at /api/v1/payments
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final MerchantRepository merchantRepository;
    private final QrCodeService qrCodeService;

    public PaymentController(PaymentService paymentService,
                               MerchantRepository merchantRepository,
                               QrCodeService qrCodeService) {
        this.paymentService = paymentService;
        this.merchantRepository = merchantRepository;
        this.qrCodeService = qrCodeService;
    }

    /**
     * Create a new payment order
     * POST /api/v1/payments
     */
    @PostMapping
    public ResponseEntity<CreatePaymentResponseV1> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        logger.info("Creating payment for merchant: {}", request.getMerchantId());

        // Find merchant
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + request.getMerchantId()));

        // Convert amount to paise (amount is in rupees)
        Long amountInPaise = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // Create payment order
        Payment payment = paymentService.createPayment(
                merchant,
                amountInPaise,
                request.getMerchantOrderId(),
                null, // description
                15    // expiry in minutes
        );

        // Build response
        CreatePaymentResponseV1 response = toCreatePaymentResponse(payment);
        response.setTestMode(request.getTestMode());
        response.setMessage("Payment created successfully");

        logger.info("Payment created: UUID={}", payment.getPaymentUuid());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate QR code for a payment
     * POST /api/v1/payments/{paymentId}/qr
     */
    @PostMapping("/{paymentId}/qr")
    public ResponseEntity<GenerateQRResponseV1> generateQR(@PathVariable Long paymentId) {

        logger.info("Generating QR for payment ID: {}", paymentId);

        // Get payment by ID (need to find by UUID or ID)
        Payment payment = paymentService.getPayment(String.valueOf(paymentId))
                .orElse(null);

        // If not found by paymentId as UUID, try to find by internal ID
        if (payment == null) {
            payment = findPaymentById(paymentId);
        }

        if (payment == null) {
            GenerateQRResponseV1 errorResponse = new GenerateQRResponseV1();
            errorResponse.setMessage("Payment not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Get merchant's UPI details
        Merchant merchant = payment.getMerchant();
        String upiVpa = merchant.getVpa();
        String upiName = merchant.getDisplayName() != null ? merchant.getDisplayName() : merchant.getLegalName();

        // Handle missing VPA
        if (upiVpa == null || upiVpa.isEmpty()) {
            // Use a default test VPA for testing purposes
            upiVpa = merchant.getMerchantId() + "@test";
            logger.warn("Merchant {} has no VPA configured, using test VPA: {}",
                    merchant.getMerchantId(), upiVpa);
        }

        // Generate QR code
        Payment updatedPayment = paymentService.generateQrCode(
                payment.getPaymentUuid(),
                upiVpa,
                upiName
        );

        QrCode qrCode = updatedPayment.getQrCode();

        // Build response
        GenerateQRResponseV1 response = new GenerateQRResponseV1();
        response.setPaymentId(updatedPayment.getId());
        response.setQrType("DYNAMIC");
        response.setQrImageBase64(qrCode.getQrImageBase64());
        response.setQrImageSvg(qrCode.getQrImageSvg());
        response.setUpiIntent(qrCode.getUpiIntent());
        response.setExpiresAt(qrCode.getExpiresAt());
        response.setStatus(updatedPayment.getStatus());
        response.setMessage("QR code generated successfully");

        logger.info("QR generated for payment: UUID={}", updatedPayment.getPaymentUuid());
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment status
     * GET /api/v1/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentStatusResponseV1> getPaymentStatus(@PathVariable Long paymentId) {

        logger.debug("Getting payment status for ID: {}", paymentId);

        // Try to find by UUID first, then by internal ID
        Payment payment = paymentService.getPayment(String.valueOf(paymentId))
                .orElse(null);

        if (payment == null) {
            payment = findPaymentById(paymentId);
        }

        if (payment == null) {
            PaymentStatusResponseV1 errorResponse = new PaymentStatusResponseV1();
            errorResponse.setMessage("Payment not found");
            return ResponseEntity.notFound().build();
        }

        // Build response
        PaymentStatusResponseV1 response = toPaymentStatusResponse(payment);
        return ResponseEntity.ok(response);
    }

    /**
     * Settle payment (for testing - simulates successful payment)
     * POST /api/v1/payments/{paymentId}/settle
     */
    @PostMapping("/{paymentId}/settle")
    public ResponseEntity<PaymentStatusResponseV1> settlePayment(@PathVariable Long paymentId) {

        logger.info("Settling payment ID: {}", paymentId);

        // Try to find by UUID first, then by internal ID
        Payment payment = paymentService.getPayment(String.valueOf(paymentId))
                .orElse(null);

        if (payment == null) {
            payment = findPaymentById(paymentId);
        }

        if (payment == null) {
            PaymentStatusResponseV1 errorResponse = new PaymentStatusResponseV1();
            errorResponse.setMessage("Payment not found");
            return ResponseEntity.notFound().build();
        }

        // Transition to PENDING first if in QR_GENERATED state
        if (payment.getStatus() == PaymentStatus.QR_GENERATED) {
            payment = paymentService.transitionToPending(payment.getPaymentUuid());
        }

        // Mark as success
        Payment updatedPayment = paymentService.markAsSuccess(payment.getPaymentUuid());

        // Build response
        PaymentStatusResponseV1 response = toPaymentStatusResponse(updatedPayment);
        response.setMessage("Payment settled successfully");

        logger.info("Payment settled: UUID={}", updatedPayment.getPaymentUuid());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a payment
     * POST /api/v1/payments/{paymentId}/cancel
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentStatusResponseV1> cancelPayment(@PathVariable Long paymentId) {

        logger.info("Cancelling payment ID: {}", paymentId);

        // Try to find by UUID first, then by internal ID
        Payment payment = paymentService.getPayment(String.valueOf(paymentId))
                .orElse(null);

        if (payment == null) {
            payment = findPaymentById(paymentId);
        }

        if (payment == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Payment updatedPayment = paymentService.cancelPayment(payment.getPaymentUuid());

            PaymentStatusResponseV1 response = toPaymentStatusResponse(updatedPayment);
            response.setMessage("Payment cancelled successfully");

            logger.info("Payment cancelled: UUID={}", updatedPayment.getPaymentUuid());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            PaymentStatusResponseV1 errorResponse = toPaymentStatusResponse(payment);
            errorResponse.setMessage("Payment is already in a terminal state");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Generate a static QR code for a merchant (no amount, never expires)
     * POST /api/v1/payments/static-qr?merchantId=MER001
     */
    @PostMapping("/static-qr")
    public ResponseEntity<GenerateQRResponseV1> generateStaticQR(@RequestParam String merchantId) {

        logger.info("Generating static QR for merchant: {}", merchantId);

        Merchant merchant = merchantRepository.findByMerchantId(merchantId).orElse(null);

        if (merchant == null) {
            GenerateQRResponseV1 errorResponse = new GenerateQRResponseV1();
            errorResponse.setMessage("Merchant not found: " + merchantId);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String upiVpa = merchant.getVpa();
        String upiName = merchant.getDisplayName() != null ? merchant.getDisplayName() : merchant.getLegalName();

        if (upiVpa == null || upiVpa.isEmpty()) {
            upiVpa = merchant.getMerchantId() + "@test";
            logger.warn("Merchant {} has no VPA configured, using test VPA: {}",
                    merchant.getMerchantId(), upiVpa);
        }

        QrCode qrCode = qrCodeService.generateStaticQrCode(merchant, upiVpa, upiName);

        GenerateQRResponseV1 response = new GenerateQRResponseV1();
        response.setQrType("STATIC");
        response.setQrImageBase64(qrCode.getQrImageBase64());
        response.setQrImageSvg(qrCode.getQrImageSvg());
        response.setUpiIntent(qrCode.getUpiIntent());
        response.setMessage("Static QR code generated successfully");

        logger.info("Static QR generated for merchant: {}", merchantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiate UPI collect for a payment
     * POST /api/v1/payments/upi/collect
     */
    @PostMapping("/upi/collect")
    public ResponseEntity<UpiCollectResponse> upiCollect(@RequestBody java.util.Map<String, Object> request) {

        Long paymentId = ((Number) request.get("paymentId")).longValue();
        String customerVpa = (String) request.get("customerVpa");

        logger.info("UPI collect for paymentId={}, customerVpa={}", paymentId, customerVpa);

        Transaction txn = paymentService.initiateUpiCollect(paymentId, customerVpa);

        UpiCollectResponse response = new UpiCollectResponse();
        response.setTransactionId(txn.getId());
        response.setTransactionUuid(txn.getTransactionUuid());
        response.setPaymentId(paymentId);
        response.setStatus(txn.getStatus());
        response.setPspReferenceId(txn.getPspTransactionId());
        response.setFailureReason(txn.getFailureReason());
        response.setCreatedAt(txn.getCreatedAt());
        response.setMessage("UPI collect initiated successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * UPI callback endpoint for simulator
     * POST /api/v1/payments/upi/callback
     */
    @PostMapping("/upi/callback")
    public ResponseEntity<Void> upiCallback(@RequestBody UpiCallbackRequest request) {

        logger.info("UPI callback received: transactionId={}, status={}",
                request.getTransactionId(), request.getStatus());

        paymentService.processUpiCallback(
                request.getTransactionId(),
                request.getStatus(),
                request.getBankReferenceId(),
                request.getFailureReason()
        );

        return ResponseEntity.ok().build();
    }

    // Helper method to find payment by internal ID
    private Payment findPaymentById(Long paymentId) {
        return paymentService.getPaymentById(paymentId).orElse(null);
    }

    private CreatePaymentResponseV1 toCreatePaymentResponse(Payment payment) {
        CreatePaymentResponseV1 response = new CreatePaymentResponseV1();
        response.setPaymentId(payment.getId());
        response.setPaymentUuid(payment.getPaymentUuid());
        response.setStatus(payment.getStatus());
        response.setDueAmount(payment.getAmountInRupees());
        response.setPaidAmount(BigDecimal.ZERO);
        response.setRefundedAmount(BigDecimal.ZERO);
        response.setCurrency(payment.getCurrency());
        response.setMerchantOrderId(payment.getExternalOrderId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setExpiresAt(payment.getExpiresAt());
        return response;
    }

    private PaymentStatusResponseV1 toPaymentStatusResponse(Payment payment) {
        PaymentStatusResponseV1 response = new PaymentStatusResponseV1();
        response.setPaymentId(payment.getId());
        response.setPaymentUuid(payment.getPaymentUuid());
        response.setStatus(payment.getStatus());
        response.setDueAmount(payment.getAmountInRupees());

        // Set paid amount based on status
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            response.setPaidAmount(payment.getAmountInRupees());
        } else {
            response.setPaidAmount(BigDecimal.ZERO);
        }

        response.setRefundedAmount(BigDecimal.ZERO);
        response.setCurrency(payment.getCurrency());
        response.setMerchantOrderId(payment.getExternalOrderId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setExpiresAt(payment.getExpiresAt());
        return response;
    }
}
