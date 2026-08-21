package com.mustycodified.musty_inventory_service.services;

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
 * Relays rows from the local {@code outbox} table to Kafka. Split into two tiers of failure
 * handling by {@link #markFailedAttempt}: a transient failure (e.g. Kafka is down) leaves the row
 * PENDING with a growing backoff so it self-heals on a later poll tick; a row that has exhausted
 * its retry budget is flipped to FAILED with a best-effort publish to the DLQ topic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private static final int MAX_RETRIES = 20;
    private static final int MAX_BACKOFF_SECONDS = 300;
    private static final String DLQ_TOPIC = "INVENTORY_OUTBOX_DLQ_TOPIC";

    @Qualifier("mySqlJdbcTemplate")
    private final JdbcTemplate mySqlJdbcTemplate;
    private final ProducerTemplate producerTemplate;

    //    the Camel outbox publisher reads unpublished entries — see OutboxRelayRouteBuilder.
    public List<OutboxRow> fetchPending() {
        return mySqlJdbcTemplate.query(
                "SELECT id, aggregate_type, aggregate_id, event_type, topic, payload, retry_count " +
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
        mySqlJdbcTemplate.update(
                "UPDATE outbox SET status = 'PUBLISHED', published_at = NOW() WHERE id = ?", row.getId());
        log.info("Outbox row published → id: {}, topic: {}, aggregateId: {}", row.getId(), row.getTopic(), row.getAggregateId());
    }

    public void markFailedAttempt(OutboxRow row, Throwable error) {
        int nextRetryCount = row.getRetryCount() + 1;
        String errorMessage = error == null ? "unknown error" : String.valueOf(error.getMessage());

        if (nextRetryCount >= MAX_RETRIES) {
            mySqlJdbcTemplate.update(
                    "UPDATE outbox SET status = 'FAILED', retry_count = ?, last_error = ? WHERE id = ?",
                    nextRetryCount, truncate(errorMessage), row.getId());
            log.error("Outbox row exhausted retries — moving to DLQ → id: {}, topic: {}, aggregateId: {}",
                    row.getId(), row.getTopic(), row.getAggregateId());
            publishToDlq(row, errorMessage);
            return;
        }

        int backoffSeconds = (int) Math.min(Math.pow(2, nextRetryCount), MAX_BACKOFF_SECONDS);
        mySqlJdbcTemplate.update(
                "UPDATE outbox SET retry_count = ?, next_attempt_at = DATE_ADD(NOW(), INTERVAL ? SECOND), last_error = ? " +
                        "WHERE id = ?",
                nextRetryCount, backoffSeconds, truncate(errorMessage), row.getId());
        log.warn("Outbox row publish failed, will retry in {}s → id: {}, topic: {}, attempt: {}",
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
            // Best-effort only — a broker outage means this publish fails too; the row is
            // already marked FAILED in the DB, which is the durable record of the poison row.
            log.error("Failed to publish poison outbox row to DLQ (best-effort) → id: {}", row.getId(), dlqPublishFailure);
        }
    }

    private String truncate(String message) {
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
