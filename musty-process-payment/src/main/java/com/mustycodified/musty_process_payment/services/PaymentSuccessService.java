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
 * Records a successful payment and its outbound PAYMENT_SUCCESS_TOPIC event atomically, via the
 * same outbox mechanism as {@link PaymentFailureService}. Used by both the mock-gateway success
 * path in AppRouteBuilder and the real Paystack webhook — previously the mock-gateway path did
 * this as two separate non-transactional steps (SQL update, then a direct Kafka publish), which
 * had the same lost-event-on-crash risk PaymentFailureService was introduced to close.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSuccessService {

    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate postgresJdbcTemplate;
    private final TransactionTemplate postgresTransactionTemplate;
    private final ObjectMapper objectMapper;

    public void recordSuccess(String paymentReference, String orderId) {
        postgresTransactionTemplate.executeWithoutResult(status -> {
            postgresJdbcTemplate.update(
                    "UPDATE payments SET payment_status='SUCCESS' WHERE payment_reference = CAST(? AS UUID)",
                    paymentReference);

            try {
                String json = objectMapper.writeValueAsString(Map.of("orderId", orderId, "status", "SUCCESS"));
                postgresJdbcTemplate.update(
                        "INSERT INTO payments_outbox (aggregate_type, aggregate_id, event_type, topic, payload) " +
                                "VALUES ('PAYMENT', ?, 'PAYMENT_SUCCEEDED', 'PAYMENT_SUCCESS_TOPIC', ?::jsonb)",
                        orderId, json);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize PAYMENT_STATUS payload for orderId " + orderId, e);
            }

            log.info("Payment marked SUCCESS, status event queued → orderId: {}, paymentReference: {}", orderId, paymentReference);
        });
    }
}
