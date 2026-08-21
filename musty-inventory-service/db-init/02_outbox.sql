-- Transactional outbox: events written here commit atomically with the business change that
-- produced them (same local JDBC transaction), then a relay route publishes them to Kafka.
CREATE TABLE IF NOT EXISTS outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    payload         JSON         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP    NULL,
    last_error      VARCHAR(500) NULL,
    INDEX idx_outbox_pending (status, next_attempt_at)
);
