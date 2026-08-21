-- Idempotent-consumer dedup guard, same shape as musty-inventory-service's inbox table. Used by
-- WebhookInboxService to guard against Paystack's webhook retry behavior (duplicate deliveries).
CREATE TABLE IF NOT EXISTS inbox (
    id             BIGSERIAL    PRIMARY KEY,
    consumer_route VARCHAR(100) NOT NULL,
    dedup_key      VARCHAR(150) NOT NULL,
    processed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inbox_route_key UNIQUE (consumer_route, dedup_key)
);
