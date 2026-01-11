package org.personal.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.personal.entity.PaymentOrder;
import org.personal.entity.QrCode;
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
     * @param payment PaymentOrder entity
     * @param upiVpa Merchant's UPI VPA (e.g., "storea@axis")
     * @param upiMerchantName Merchant's UPI display name
     * @return Generated QrCode entity
     */
    public QrCode generateQrCode(PaymentOrder payment, String upiVpa, String upiMerchantName) {
        try {
            // Generate UPI intent URI
            String upiIntent = buildUpiIntent(payment, upiVpa, upiMerchantName);

            // Generate QR code images
            String qrBase64 = generateQrCodeBase64(upiIntent);
            String qrSvg = generateQrCodeSvg(upiIntent);

            // Create QrCode entity
            QrCode qrCode = new QrCode();
            qrCode.setPaymentOrder(payment);
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
            throw new RuntimeException("QR generation failed", e);
        }
    }

    /**
     * Build UPI intent URI
     * Format: upi://pay?pa=storea@axis&pn=Store%20A&am=100.50&tr=ORDER-123&tn=Coffee
     * @param payment PaymentOrder entity
     * @param upiVpa Merchant's UPI VPA
     * @param upiMerchantName Merchant's UPI display name
     * @return UPI intent string
     */
    private String buildUpiIntent(PaymentOrder payment, String upiVpa, String upiMerchantName) {
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
     * Get QR code for a payment
     * @param payment PaymentOrder entity
     * @return QrCode if found
     */
    public QrCode getQrCodeForPayment(PaymentOrder payment) {
        return qrCodeRepository.findByPaymentOrder(payment)
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
