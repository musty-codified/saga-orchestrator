-- Idempotent-consumer dedup guard: a Kafka consumer claims a message by inserting here before
-- acting on it. A duplicate-key violation means the message was already processed — skip it.
CREATE TABLE IF NOT EXISTS inbox (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    consumer_route VARCHAR(100) NOT NULL,
    dedup_key      VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inbox_route_key UNIQUE (consumer_route, dedup_key)
);
