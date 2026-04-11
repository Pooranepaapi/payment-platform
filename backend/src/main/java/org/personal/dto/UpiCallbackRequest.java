package org.personal.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpiCallbackRequest {

    private String transactionId;
    private String pspReferenceId;
    private String bankReferenceId;
    private String status;
    private String failureReason;
}
