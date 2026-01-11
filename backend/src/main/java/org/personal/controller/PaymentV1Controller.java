package org.personal.controller;

import jakarta.validation.Valid;
import org.personal.dto.*;
import org.personal.entity.Merchant;
import org.personal.entity.PaymentOrder;
import org.personal.entity.QrCode;
import org.personal.enums.PaymentStatus;
import org.personal.repository.MerchantRepository;
import org.personal.service.PaymentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Payment V1 Controller
 * Handles QR payment flow endpoints at /api/v1/payments
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller {

    private static final Logger logger = LoggerFactory.getLogger(PaymentV1Controller.class);

    private final PaymentOrderService paymentOrderService;
    private final MerchantRepository merchantRepository;

    public PaymentV1Controller(PaymentOrderService paymentOrderService,
                               MerchantRepository merchantRepository) {
        this.paymentOrderService = paymentOrderService;
        this.merchantRepository = merchantRepository;
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
        PaymentOrder payment = paymentOrderService.createPaymentOrder(
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
        PaymentOrder payment = paymentOrderService.getPaymentOrder(String.valueOf(paymentId))
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
        PaymentOrder updatedPayment = paymentOrderService.generateQrCode(
                payment.getPaymentUuid(),
                upiVpa,
                upiName
        );

        QrCode qrCode = updatedPayment.getQrCode();

        // Build response
        GenerateQRResponseV1 response = new GenerateQRResponseV1();
        response.setPaymentId(updatedPayment.getId());
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
        PaymentOrder payment = paymentOrderService.getPaymentOrder(String.valueOf(paymentId))
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
        PaymentOrder payment = paymentOrderService.getPaymentOrder(String.valueOf(paymentId))
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
            payment = paymentOrderService.transitionToPending(payment.getPaymentUuid());
        }

        // Mark as success
        PaymentOrder updatedPayment = paymentOrderService.markAsSuccess(payment.getPaymentUuid());

        // Build response
        PaymentStatusResponseV1 response = toPaymentStatusResponse(updatedPayment);
        response.setMessage("Payment settled successfully");

        logger.info("Payment settled: UUID={}", updatedPayment.getPaymentUuid());
        return ResponseEntity.ok(response);
    }

    // Helper method to find payment by internal ID
    private PaymentOrder findPaymentById(Long paymentId) {
        return paymentOrderService.getPaymentOrderById(paymentId).orElse(null);
    }

    private CreatePaymentResponseV1 toCreatePaymentResponse(PaymentOrder payment) {
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

    private PaymentStatusResponseV1 toPaymentStatusResponse(PaymentOrder payment) {
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
