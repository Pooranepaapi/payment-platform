package org.personal.simulator.controller;

import jakarta.validation.Valid;
import org.personal.simulator.dto.upi.*;
import org.personal.simulator.enums.PaymentMethod;
import org.personal.simulator.enums.PaymentType;
import org.personal.simulator.factory.BankSimulatorFactory;
import org.personal.simulator.service.UpiSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for UPI simulation endpoints.
 * These endpoints are called by the payment backend when in test mode.
 */
@RestController
@RequestMapping("/api/simulator/upi")
public class UpiSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(UpiSimulatorController.class);

    private final BankSimulatorFactory factory;

    public UpiSimulatorController(BankSimulatorFactory factory) {
        this.factory = factory;
    }

    /**
     * POST /api/simulator/upi/collect
     * Initiates a UPI collect request simulation.
     *
     * Called by backend's PaymentService when initiating UPI collect in test mode.
     */
    @PostMapping("/collect")
    public ResponseEntity<UpiCollectSimulatorResponse> initiateCollect(
            @Valid @RequestBody UpiCollectSimulatorRequest request) {

        log.info("Received collect request: txnId={}, paymentType={}, customerVpa={}",
            request.getTransactionId(), request.getPaymentType(), request.getCustomerVpa());

        UpiSimulator simulator = factory.getUpiSimulator(request.getPaymentType());
        UpiCollectSimulatorResponse response = simulator.initiateCollect(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/simulator/upi/refund
     * Initiates a UPI refund simulation.
     */
    @PostMapping("/refund")
    public ResponseEntity<UpiRefundSimulatorResponse> initiateRefund(
            @Valid @RequestBody UpiRefundSimulatorRequest request) {

        log.info("Received refund request: txnId={}, originalTxnId={}",
            request.getTransactionId(), request.getOriginalTransactionId());

        UpiSimulator simulator = factory.getUpiSimulator(request.getPaymentType());
        UpiRefundSimulatorResponse response = simulator.initiateRefund(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/simulator/upi/status
     * Check transaction status (for reconciliation).
     */
    @PostMapping("/status")
    public ResponseEntity<UpiCollectSimulatorResponse> checkStatus(
            @Valid @RequestBody UpiStatusCheckRequest request) {

        log.info("Received status check: pspRef={}", request.getPspReferenceId());

        UpiSimulator simulator = factory.getUpiSimulator(request.getPaymentType());
        UpiCollectSimulatorResponse response = simulator.checkStatus(request);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/simulator/upi/supported-banks
     * Returns list of supported UPI payment types.
     */
    @GetMapping("/supported-banks")
    public ResponseEntity<Map<String, Object>> getSupportedBanks() {
        List<PaymentType> supported = factory.getSimulatorsByMethod(PaymentMethod.UPI)
            .stream()
            .map(s -> s.getPaymentType())
            .toList();

        return ResponseEntity.ok(Map.of(
            "supportedBanks", supported,
            "count", supported.size()
        ));
    }

    /**
     * GET /api/simulator/upi/health
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "UPI Simulator"
        ));
    }

    /**
     * GET /api/simulator/upi/test-vpas
     * Returns available test VPAs and their behaviors.
     */
    @GetMapping("/test-vpas")
    public ResponseEntity<Map<String, Object>> getTestVpas() {
        Map<String, String> commonVpas = Map.of(
            "success@upi", "SUCCESS - Always succeeds",
            "fail@upi", "FAILED - Customer rejects",
            "timeout@upi", "PENDING - No callback (stays pending)",
            "insufficient@upi", "FAILED - Insufficient funds"
        );

        Map<String, String> rblVpas = Map.of(
            "rbl.success@rbl", "SUCCESS - RBL-specific success",
            "rbl.fail@rbl", "FAILED - RBL-specific failure",
            "rbl.timeout@rbl", "PENDING - RBL-specific timeout",
            "rbl.lowbal@rbl", "FAILED - RBL low balance"
        );

        Map<String, String> hdfcVpas = Map.of(
            "hdfc.success@hdfc", "SUCCESS - HDFC-specific success",
            "hdfc.fail@hdfc", "FAILED - HDFC-specific failure",
            "hdfc.timeout@hdfc", "PENDING - HDFC-specific timeout",
            "hdfc.declined@hdfc", "FAILED - HDFC declined",
            "hdfc.nobal@hdfc", "FAILED - HDFC no balance"
        );

        Map<String, String> kotakVpas = Map.of(
            "kotak.success@kotak", "SUCCESS - Kotak-specific success",
            "kotak.fail@kotak", "FAILED - Kotak-specific failure",
            "kotak.timeout@kotak", "PENDING - Kotak-specific timeout",
            "kotak.blocked@kotak", "FAILED - Kotak blocked VPA"
        );

        return ResponseEntity.ok(Map.of(
            "common", commonVpas,
            "RBL", rblVpas,
            "HDFC", hdfcVpas,
            "KOTAK", kotakVpas
        ));
    }
}
