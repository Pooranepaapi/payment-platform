package org.personal.service;

import org.personal.entity.PaymentEvent;
import org.personal.entity.Payment;
import org.personal.enums.PaymentEventType;
import org.personal.enums.PaymentStatus;
import org.personal.repository.PaymentEventRepository;
import org.personal.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "qr.expiry.job.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentExpiryJob {

    private static final Logger logger = LoggerFactory.getLogger(PaymentExpiryJob.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentExpiryJob(PaymentRepository paymentRepository,
                            PaymentEventRepository paymentEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Scheduled(fixedDelayString = "${qr.expiry.job.interval-ms:60000}")
    @Transactional
    public void expirePayments() {
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(LocalDateTime.now());

        if (expiredPayments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<PaymentEvent> events = new ArrayList<>();

        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.EXPIRED);
            payment.setUpdatedAt(now);

            PaymentEvent event = new PaymentEvent();
            event.setPayment(payment);
            event.setPaymentUuid(payment.getPaymentUuid());
            event.setEventType(PaymentEventType.EXPIRED);
            event.setCreatedAt(now);
            events.add(event);
        }

        paymentRepository.saveAll(expiredPayments);
        paymentEventRepository.saveAll(events);

        logger.info("Expiry job: marked {} payments as EXPIRED", expiredPayments.size());
    }
}
