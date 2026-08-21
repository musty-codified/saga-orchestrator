package com.mustycodified.commonlib.models.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Row shape shared by every service-local {@code outbox} table. Each service owns its own
 * physical table (dialect-specific DDL — MySQL vs Postgres) but the relay logic that fetches
 * pending rows and republishes them to Kafka operates on this common shape.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxRow {
    private Long id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String topic;
    private String payload;
    private OutboxStatus status;
    private int retryCount;
    private Instant nextAttemptAt;
    private String lastError;
}
