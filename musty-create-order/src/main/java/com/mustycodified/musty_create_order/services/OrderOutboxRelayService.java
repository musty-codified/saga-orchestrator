package com.mustycodified.musty_create_order.services;

import com.mustycodified.commonlib.models.outbox.OutboxRow;
import com.mustycodified.commonlib.models.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Relays rows from the local {@code outbox} table to Kafka. Mirrors musty-inventory-service's
 * OutboxRelayService / musty-process-payment's PaymentOutboxRelayService — see either for the
 * two-tiered transient-vs-poison-row retry rationale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxRelayService {

    private static final int MAX_RETRIES = 20;
    private static final int MAX_BACKOFF_SECONDS = 300;
    private static final String DLQ_TOPIC = "ORDER_OUTBOX_DLQ_TOPIC";

    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate postgresJdbcTemplate;
    private final ProducerTemplate producerTemplate;

    public List<OutboxRow> fetchPending() {
        return postgresJdbcTemplate.query(
                "SELECT id, aggregate_type, aggregate_id, event_type, topic, payload::text AS payload, retry_count " +
                        "FROM outbox WHERE status = 'PENDING' " +
                        "AND (next_attempt_at IS NULL OR next_attempt_at <= NOW()) " +
                        "ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED",
                (rs, rowNum) -> OutboxRow.builder()
                        .id(rs.getLong("id"))
                        .aggregateType(rs.getString("aggregate_type"))
                        .aggregateId(rs.getString("aggregate_id"))
                        .eventType(rs.getString("event_type"))
                        .topic(rs.getString("topic"))
                        .payload(rs.getString("payload"))
                        .status(OutboxStatus.PENDING)
                        .retryCount(rs.getInt("retry_count"))
                        .build());
    }

    public void markPublished(OutboxRow row) {
        postgresJdbcTemplate.update(
                "UPDATE outbox SET status = 'PUBLISHED', published_at = NOW() WHERE id = ?", row.getId());
        log.info("Orders outbox row published → id: {}, topic: {}, aggregateId: {}", row.getId(), row.getTopic(), row.getAggregateId());
    }

    public void markFailedAttempt(OutboxRow row, Throwable error) {
        int nextRetryCount = row.getRetryCount() + 1;
        String errorMessage = error == null ? "unknown error" : String.valueOf(error.getMessage());

        if (nextRetryCount >= MAX_RETRIES) {
            postgresJdbcTemplate.update(
                    "UPDATE outbox SET status = 'FAILED', retry_count = ?, last_error = ? WHERE id = ?",
                    nextRetryCount, truncate(errorMessage), row.getId());
            log.error("Orders outbox row exhausted retries — moving to DLQ → id: {}, topic: {}, aggregateId: {}",
                    row.getId(), row.getTopic(), row.getAggregateId());
            publishToDlq(row, errorMessage);
            return;
        }

        int backoffSeconds = (int) Math.min(Math.pow(2, nextRetryCount), MAX_BACKOFF_SECONDS);
        postgresJdbcTemplate.update(
                "UPDATE outbox SET retry_count = ?, next_attempt_at = NOW() + (? || ' seconds')::interval, last_error = ? " +
                        "WHERE id = ?",
                nextRetryCount, backoffSeconds, truncate(errorMessage), row.getId());
        log.warn("Orders outbox row publish failed, will retry in {}s → id: {}, topic: {}, attempt: {}",
                backoffSeconds, row.getId(), row.getTopic(), nextRetryCount);
    }

    private void publishToDlq(OutboxRow row, String errorMessage) {
        try {
            producerTemplate.sendBodyAndHeader(
                    "kafka:" + DLQ_TOPIC + "?brokers={{spring.kafka.bootstrap-servers}}",
                    Map.of(
                            "originalTopic", row.getTopic(),
                            "aggregateId", row.getAggregateId(),
                            "eventType", row.getEventType(),
                            "payload", row.getPayload(),
                            "error", errorMessage
                    ).toString(),
                    "outboxRowId", row.getId());
        } catch (Exception dlqPublishFailure) {
            log.error("Failed to publish poison orders_outbox row to DLQ (best-effort) → id: {}", row.getId(), dlqPublishFailure);
        }
    }

    private String truncate(String message) {
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}