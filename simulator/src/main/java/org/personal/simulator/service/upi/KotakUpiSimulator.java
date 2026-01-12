package org.personal.simulator.service.upi;

import org.personal.simulator.enums.PaymentType;
import org.personal.simulator.service.AbstractUpiSimulator;
import org.personal.simulator.service.CallbackService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kotak Bank UPI Simulator.
 *
 * Bank-specific behaviors:
 * - Slightly slower processing (2500ms callback delay)
 * - Kotak-specific test VPAs
 * - PSP Reference format: KOTAKPSP...
 */
@Component
public class KotakUpiSimulator extends AbstractUpiSimulator {

    // Kotak-specific test VPAs
    private static final Map<String, String> KOTAK_VPA_BEHAVIORS = Map.of(
        "kotak.success@kotak", "SUCCESS",
        "kotak.fail@kotak", "FAILED",
        "kotak.timeout@kotak", "PENDING",
        "kotak.blocked@kotak", "FAILED"
    );

    public KotakUpiSimulator(CallbackService callbackService) {
        super(callbackService);
        log.info("Kotak UPI Simulator initialized");
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.KOTAKUPI;
    }

    @Override
    protected String getBankPrefix() {
        return "KOTAK";
    }

    @Override
    protected Map<String, String> getBankSpecificVpaBehaviors() {
        return KOTAK_VPA_BEHAVIORS;
    }

    @Override
    protected long getCallbackDelayMs() {
        return 2500; // Kotak slightly slower
    }
}
