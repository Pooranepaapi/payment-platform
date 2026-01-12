package org.personal.simulator.service.upi;

import org.personal.simulator.enums.PaymentType;
import org.personal.simulator.service.AbstractUpiSimulator;
import org.personal.simulator.service.CallbackService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RBL Bank UPI Simulator.
 *
 * Bank-specific behaviors:
 * - Faster processing (1500ms callback delay)
 * - Additional test VPAs: rbl.success@rbl, rbl.fail@rbl, etc.
 * - PSP Reference format: RBLPSP...
 */
@Component
public class RblUpiSimulator extends AbstractUpiSimulator {

    // RBL-specific test VPAs
    private static final Map<String, String> RBL_VPA_BEHAVIORS = Map.of(
        "rbl.success@rbl", "SUCCESS",
        "rbl.fail@rbl", "FAILED",
        "rbl.timeout@rbl", "PENDING",
        "rbl.lowbal@rbl", "INSUFFICIENT_FUNDS"
    );

    public RblUpiSimulator(CallbackService callbackService) {
        super(callbackService);
        log.info("RBL UPI Simulator initialized");
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.RBLUPI;
    }

    @Override
    protected String getBankPrefix() {
        return "RBL";
    }

    @Override
    protected Map<String, String> getBankSpecificVpaBehaviors() {
        return RBL_VPA_BEHAVIORS;
    }

    @Override
    protected long getCallbackDelayMs() {
        return 1500; // RBL processes faster
    }
}
