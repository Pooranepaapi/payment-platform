package org.personal.service;

import tools.jackson.databind.ObjectMapper;
import org.personal.enums.PaymentType;
import org.personal.enums.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client for communicating with the external simulator service.
 */
@Service
public class SimulatorClient {

    private static final Logger log = LoggerFactory.getLogger(SimulatorClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${simulator.base-url:http://localhost:8181}")
    private String simulatorBaseUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public SimulatorClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Initiate UPI collect request via simulator.
     */
    public SimulatorResponse initiateUpiCollect(
            String transactionId,
            BigDecimal amount,
            String customerVpa,
            String merchantVpa,
            PaymentType paymentType,
            String callbackUrl) {

        String url = simulatorBaseUrl + "/api/simulator/upi/collect";

        Map<String, Object> request = new HashMap<>();
        request.put("transactionId", transactionId);
        request.put("paymentType", paymentType.name());
        request.put("amount", amount);
        request.put("customerVpa", customerVpa);
        request.put("merchantVpa", merchantVpa);
        request.put("callbackUrl", callbackUrl);
        request.put("currency", "INR");

        try {
            log.info("Calling simulator collect: url={}, txnId={}, paymentType={}",
                url, transactionId, paymentType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = response.getBody();

            SimulatorResponse result = new SimulatorResponse();
            result.setPspReferenceId((String) body.get("pspReferenceId"));
            result.setStatus(TransactionStatus.valueOf((String) body.get("status")));
            result.setFailureReason((String) body.get("failureReason"));
            result.setBankCode((String) body.get("bankCode"));

            log.info("Simulator collect response: pspRef={}, status={}, bankCode={}",
                result.getPspReferenceId(), result.getStatus(), result.getBankCode());

            return result;

        } catch (Exception e) {
            log.error("Failed to call simulator: {}", e.getMessage());
            throw new RuntimeException("Simulator unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Check if simulator is available.
     */
    public boolean isSimulatorAvailable() {
        try {
            String url = simulatorBaseUrl + "/api/simulator/upi/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Simulator health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Response from simulator.
     */
    public static class SimulatorResponse {
        private String pspReferenceId;
        private String bankReferenceId;
        private TransactionStatus status;
        private String failureReason;
        private String bankCode;

        public String getPspReferenceId() { return pspReferenceId; }
        public void setPspReferenceId(String pspReferenceId) { this.pspReferenceId = pspReferenceId; }

        public String getBankReferenceId() { return bankReferenceId; }
        public void setBankReferenceId(String bankReferenceId) { this.bankReferenceId = bankReferenceId; }

        public TransactionStatus getStatus() { return status; }
        public void setStatus(TransactionStatus status) { this.status = status; }

        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

        public String getBankCode() { return bankCode; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    }
}
