package org.personal.simulator.service.upi;

import org.personal.simulator.enums.PaymentType;
import org.personal.simulator.service.AbstractUpiSimulator;
import org.personal.simulator.service.CallbackService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HDFC Bank UPI Simulator.
 *
 * Bank-specific behaviors:
 * - Standard processing (2000ms callback delay)
 * - Additional test VPAs with HDFC-specific error codes
 * - PSP Reference format: HDFCPSP...
 */
@Component
public class HdfcUpiSimulator extends AbstractUpiSimulator {

    // HDFC-specific test VPAs
    private static final Map<String, String> HDFC_VPA_BEHAVIORS = Map.of(
        "hdfc.success@hdfc", "SUCCESS",
        "hdfc.fail@hdfc", "FAILED",
        "hdfc.timeout@hdfc", "PENDING",
        "hdfc.declined@hdfc", "FAILED",
        "hdfc.nobal@hdfc", "INSUFFICIENT_FUNDS"
    );

    public HdfcUpiSimulator(CallbackService callbackService) {
        super(callbackService);
        log.info("HDFC UPI Simulator initialized");
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.HDFCUPI;
    }

    @Override
    protected String getBankPrefix() {
        return "HDFC";
    }

    @Override
    protected Map<String, String> getBankSpecificVpaBehaviors() {
        return HDFC_VPA_BEHAVIORS;
    }

    @Override
    protected long getCallbackDelayMs() {
        return 2000; // Standard delay
    }
}
