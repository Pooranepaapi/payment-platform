package org.personal.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.jspecify.annotations.NonNull;
import org.personal.entity.Merchant;
import org.personal.entity.Payment;
import org.personal.entity.QrCode;
import org.personal.exception.PaymentException;
import org.personal.enums.QrType;
import org.personal.repository.QrCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * QrCodeService
 * Generates and manages QR codes for payments
 * 
 * Responsibilities:
 * - Generate UPI dynamic QR codes (with amount)
 * - Generate Base64 PNG and SVG formats
 * - Generate UPI intent URIs (fallback)
 * - Check QR expiry
 */
@Service
@Transactional
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);

    private final QrCodeRepository qrCodeRepository;

    @Value("${qr.size.pixels:400}")
    private int qrSize;

    public QrCodeService(QrCodeRepository qrCodeRepository) {
        this.qrCodeRepository = qrCodeRepository;
    }

    /**
     * Generate QR code for a payment
     * @param payment Payment entity
     * @param upiVpa Merchant's UPI VPA (e.g., "storea@axis")
     * @param upiMerchantName Merchant's UPI display name
     * @return Generated QrCode entity
     */
    public QrCode generateQrCode(Payment payment, String upiVpa, String upiMerchantName) {
        try {
            // Generate UPI intent URI
            String upiIntent = buildUpiIntent(payment, upiVpa, upiMerchantName);

            // Generate QR code images
            String qrBase64 = generateQrCodeBase64(upiIntent);
            String qrSvg = generateQrCodeSvg(upiIntent);

            // Create QrCode entity
            QrCode qrCode = new QrCode();
            qrCode.setPayment(payment);
            qrCode.setQrType(QrType.DYNAMIC);
            qrCode.setQrImageBase64(qrBase64);
            qrCode.setQrImageSvg(qrSvg);
            qrCode.setUpiIntent(upiIntent);
            qrCode.setExpiresAt(payment.getExpiresAt());
            qrCode.setCreatedAt(LocalDateTime.now());
            qrCode.setUpdatedAt(LocalDateTime.now());

            QrCode saved = qrCodeRepository.save(qrCode);
            logger.info("QR code generated for payment: {}", payment.getPaymentUuid());

            return saved;
        } catch (WriterException e) {
            logger.error("Failed to generate QR code for payment: {}", payment.getPaymentUuid(), e);
            throw new PaymentException("QR generation failed", "QR_GENERATION_FAILED");
        }
    }

    /**
     * Build UPI intent URI
     * Format: upi://pay?pa=storea@axis&pn=Store%20A&am=100.50&tr=ORDER-123&tn=Coffee
     * @param payment Payment entity
     * @param upiVpa Merchant's UPI VPA
     * @param upiMerchantName Merchant's UPI display name
     * @return UPI intent string
     */
    private String buildUpiIntent(Payment payment, String upiVpa, String upiMerchantName) {
        // Convert paise to rupees
        BigDecimal amountInRupees = payment.getAmountInRupees();

        // URL encode merchant name
        String encodedName = upiMerchantName.replace(" ", "%20");

        // Build UPI intent
        StringBuilder upiUri = new StringBuilder("upi://pay?");
        upiUri.append("pa=").append(upiVpa);
        upiUri.append("&pn=").append(encodedName);
        upiUri.append("&am=").append(amountInRupees);
        upiUri.append("&tr=").append(payment.getPaymentUuid());

        if (payment.getDescription() != null && !payment.getDescription().isEmpty()) {
            String encodedDesc = payment.getDescription().replace(" ", "%20");
            upiUri.append("&tn=").append(encodedDesc);
        }

        return upiUri.toString();
    }

    /**
     * Generate QR code as Base64-encoded PNG
     * @param content UPI intent URI
     * @return Base64-encoded QR code image
     * @throws WriterException if generation fails
     */
    private String generateQrCodeBase64(String content) throws WriterException {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);

            byte[] imageBytes = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            logger.error("Failed to generate Base64 QR code", e);
            throw new WriterException(e);
        }
    }

    /**
     * Generate QR code as SVG (Scalable Vector Graphics)
     * Currently returns placeholder - full SVG generation in Phase 2
     * @param content UPI intent URI
     * @return SVG string
     */
    private String generateQrCodeSvg(String content) {
        // Phase 2: Implement full SVG generation
        // For now, return empty SVG placeholder
        return "<svg width=\"" + qrSize + "\" height=\"" + qrSize + "\" xmlns=\"http://www.w3.org/2000/svg\">" +
               "<rect width=\"" + qrSize + "\" height=\"" + qrSize + "\" fill=\"white\"/>" +
               "<text x=\"50%\" y=\"50%\" text-anchor=\"middle\" dy=\"0.3em\" font-size=\"12\">QR Code SVG (Phase 2)</text>" +
               "</svg>";
    }

    /**
     * Generate or retrieve a static QR code for a merchant.
     * Static QRs have no amount — the customer enters the amount in their UPI app.
     * One static QR per merchant; returns existing if already generated.
     */
    public QrCode generateStaticQrCode(Merchant merchant, String upiVpa, String upiMerchantName) {
        // Return existing static QR if one exists for this merchant
        return qrCodeRepository.findByMerchantAndQrType(merchant, QrType.STATIC)
                .orElseGet(() -> {
                    try {
                        return getQrCode(merchant, upiVpa, upiMerchantName);
                    } catch (WriterException e) {
                        logger.error("Failed to generate static QR code for merchant: {}", merchant.getMerchantId(), e);
                        throw new PaymentException("Static QR generation failed", "QR_GENERATION_FAILED");
                    }
                });
    }

    private @NonNull QrCode getQrCode(Merchant merchant, String upiVpa, String upiMerchantName) throws WriterException {
        String upiIntent = buildStaticUpiIntent(upiVpa, upiMerchantName);
        String qrBase64 = generateQrCodeBase64(upiIntent);
        String qrSvg = generateQrCodeSvg(upiIntent);

        QrCode qrCode = new QrCode();
        qrCode.setMerchant(merchant);
        qrCode.setQrType(QrType.STATIC);
        qrCode.setQrImageBase64(qrBase64);
        qrCode.setQrImageSvg(qrSvg);
        qrCode.setUpiIntent(upiIntent);
        // Static QRs do not expire
        qrCode.setExpiresAt(null);
        qrCode.setCreatedAt(LocalDateTime.now());
        qrCode.setUpdatedAt(LocalDateTime.now());

        QrCode saved = qrCodeRepository.save(qrCode);
        logger.info("Static QR code generated for merchant: {}", merchant.getMerchantId());
        return saved;
    }

    /**
     * Build UPI intent URI for static QR (no amount, no transaction reference)
     * Format: upi://pay?pa=storea@axis&pn=Store%20A
     */
    private String buildStaticUpiIntent(String upiVpa, String upiMerchantName) {
        String encodedName = upiMerchantName.replace(" ", "%20");
        return "upi://pay?pa=" + upiVpa + "&pn=" + encodedName;
    }

    /**
     * Get QR code for a payment
     * @param payment Payment entity
     * @return QrCode if found
     */
    public QrCode getQrCodeForPayment(Payment payment) {
        return qrCodeRepository.findByPayment(payment)
                .orElseThrow(() -> new IllegalArgumentException("No QR code found for payment"));
    }

    /**
     * Check if QR code is expired
     * @param qrCode QrCode entity
     * @return true if expired
     */
    public boolean isQrCodeExpired(QrCode qrCode) {
        return qrCode.isExpired();
    }
}
