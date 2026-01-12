package org.personal.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SimulatorApplication {

    private static final Logger log = LoggerFactory.getLogger(SimulatorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);

        log.info("");
        log.info("=================================================");
        log.info("   PAYMENT SIMULATOR SERVICE");
        log.info("=================================================");
        log.info("   Port: 8181");
        log.info("");
        log.info("   Supported Banks (UPI):");
        log.info("     - RBLUPI  (RBL Bank)");
        log.info("     - HDFCUPI (HDFC Bank)");
        log.info("     - KOTAKUPI (Kotak Bank)");
        log.info("");
        log.info("   Common Test VPAs:");
        log.info("     - success@upi     : Always succeeds");
        log.info("     - fail@upi        : Always fails");
        log.info("     - timeout@upi     : Stays pending");
        log.info("     - insufficient@upi: Insufficient funds");
        log.info("");
        log.info("   Endpoints:");
        log.info("     POST /api/simulator/upi/collect");
        log.info("     POST /api/simulator/upi/refund");
        log.info("     POST /api/simulator/upi/status");
        log.info("     GET  /api/simulator/upi/health");
        log.info("=================================================");
    }
}
