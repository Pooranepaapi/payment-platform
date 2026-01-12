package org.personal.simulator.factory;

import org.personal.simulator.enums.PaymentMethod;
import org.personal.simulator.enums.PaymentType;
import org.personal.simulator.exception.SimulatorException;
import org.personal.simulator.service.BankSimulator;
import org.personal.simulator.service.UpiSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for selecting the appropriate bank simulator based on PaymentType.
 * Uses Spring's dependency injection to automatically discover all simulator implementations.
 */
@Component
public class BankSimulatorFactory {

    private static final Logger log = LoggerFactory.getLogger(BankSimulatorFactory.class);

    private final Map<PaymentType, BankSimulator> simulatorMap;

    /**
     * Constructor injection - Spring automatically injects all BankSimulator implementations.
     */
    public BankSimulatorFactory(List<BankSimulator> simulators) {
        this.simulatorMap = simulators.stream()
            .collect(Collectors.toMap(
                BankSimulator::getPaymentType,
                Function.identity(),
                (existing, replacement) -> {
                    log.warn("Duplicate simulator for {}, using {}",
                        existing.getPaymentType(), replacement.getClass().getSimpleName());
                    return replacement;
                }
            ));

        log.info("Initialized BankSimulatorFactory with {} simulators: {}",
            simulatorMap.size(), simulatorMap.keySet());
    }

    /**
     * Get a bank simulator for the specified payment type.
     * @throws SimulatorException if no simulator is registered for the payment type
     */
    public BankSimulator getSimulator(PaymentType paymentType) {
        BankSimulator simulator = simulatorMap.get(paymentType);
        if (simulator == null) {
            throw new SimulatorException(
                "No simulator registered for payment type: " + paymentType,
                "UNSUPPORTED_PAYMENT_TYPE"
            );
        }
        return simulator;
    }

    /**
     * Get a UPI-specific simulator.
     * @throws SimulatorException if simulator is not UPI type
     */
    public UpiSimulator getUpiSimulator(PaymentType paymentType) {
        BankSimulator simulator = getSimulator(paymentType);
        if (!(simulator instanceof UpiSimulator)) {
            throw new SimulatorException(
                "Payment type " + paymentType + " is not a UPI payment type",
                "INVALID_PAYMENT_METHOD"
            );
        }
        return (UpiSimulator) simulator;
    }

    /**
     * Check if a payment type is supported.
     */
    public boolean isSupported(PaymentType paymentType) {
        return simulatorMap.containsKey(paymentType);
    }

    /**
     * Get all supported payment types.
     */
    public List<PaymentType> getSupportedPaymentTypes() {
        return List.copyOf(simulatorMap.keySet());
    }

    /**
     * Get simulators by payment method.
     */
    public List<BankSimulator> getSimulatorsByMethod(PaymentMethod method) {
        return simulatorMap.values().stream()
            .filter(s -> s.getPaymentMethod() == method)
            .toList();
    }
}
