package org.personal.dto;

import lombok.Getter;
import lombok.Setter;
import org.personal.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment status
 * GET /api/v1/payments/{paymentId}
 */
@Getter
@Setter
public class PaymentStatusResponseV1 {

    private Long paymentId;
    private String paymentUuid;
    private PaymentStatus status;
    private BigDecimal dueAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private String currency;
    private String merchantOrderId;
    private Long transactionId;
    private String transactionUuid;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private String message;
}
