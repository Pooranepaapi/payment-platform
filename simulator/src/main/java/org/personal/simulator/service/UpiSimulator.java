package org.personal.simulator.service;

import org.personal.simulator.dto.upi.UpiCollectSimulatorRequest;
import org.personal.simulator.dto.upi.UpiCollectSimulatorResponse;
import org.personal.simulator.dto.upi.UpiRefundSimulatorRequest;
import org.personal.simulator.dto.upi.UpiRefundSimulatorResponse;
import org.personal.simulator.dto.upi.UpiStatusCheckRequest;

/**
 * UPI-specific simulator interface.
 */
public interface UpiSimulator extends BankSimulator {

    /**
     * Initiates a UPI collect request.
     * Returns immediately with PSP reference, triggers async callback.
     */
    UpiCollectSimulatorResponse initiateCollect(UpiCollectSimulatorRequest request);

    /**
     * Initiates a UPI refund.
     */
    UpiRefundSimulatorResponse initiateRefund(UpiRefundSimulatorRequest request);

    /**
     * Checks status of a transaction (for reconciliation).
     */
    UpiCollectSimulatorResponse checkStatus(UpiStatusCheckRequest request);
}
