package org.personal.dto;

import lombok.Getter;
import lombok.Setter;
import org.personal.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment creation
 * POST /api/v1/payments
 */
@Getter
@Setter
public class CreatePaymentResponseV1 {

    private Long paymentId;
    private String paymentUuid;
    private PaymentStatus status;
    private BigDecimal dueAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private String currency;
    private String merchantOrderId;
    private Boolean testMode;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String message;
}
