package org.personal.dto;

import lombok.Getter;
import lombok.Setter;
import org.personal.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for QR code generation
 * POST /api/v1/payments/{paymentId}/qr
 */
@Getter
@Setter
public class GenerateQRResponseV1 {

    private Long paymentId;
    private String qrType;
    private String qrImageBase64;
    private String qrImageSvg;
    private String upiIntent;
    private LocalDateTime expiresAt;
    private PaymentStatus status;
    private String message;
}
