package com.mustycodified.musty_process_payment.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * Records a payment failure and its outbound PAYMENT_FAILED_TOPIC event atomically. That event is
 * the trigger for the inventory-release compensating transaction in musty-inventory-service — if
 * this write and the Kafka publish were two separate steps (as they were before), a crash between
 * them would silently drop the compensation entirely.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFailureService {

    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate postgresJdbcTemplate;
    private final TransactionTemplate postgresTransactionTemplate;
    private final ObjectMapper objectMapper;

    public void recordFailure(String paymentReference, String orderId) {
        postgresTransactionTemplate.executeWithoutResult(status -> {

            postgresJdbcTemplate.update(
            "UPDATE payments SET payment_status='FAILED' WHERE payment_reference = CAST(? AS UUID)",
                    paymentReference);

            try {
                String json = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "FAILED"));
                postgresJdbcTemplate.update(
                        "INSERT INTO payments_outbox (aggregate_type, aggregate_id, event_type, topic, payload) " +
                                "VALUES ('PAYMENT', ?, 'PAYMENT_FAILED', 'PAYMENT_FAILED_TOPIC', ?::jsonb)",
                        orderId, json);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize PAYMENT_FAILED payload for orderId " + orderId, e);
            }

            log.warn("Payment marked FAILED, compensation event queued → orderId: {}, paymentReference: {}", orderId, paymentReference);
        });
    }
}
