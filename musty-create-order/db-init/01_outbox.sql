-- Transactional outbox for musty-create-order: the INVENTORY_RESERVE_TOPIC event is written here
-- atomically with the orders INSERT, instead of being published as a separate, crash-vulnerable
-- step. Same shape as musty-process-payment's payments_outbox (Postgres).
CREATE TABLE IF NOT EXISTS outbox (
    id              BIGSERIAL    PRIMARY KEY,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP    NULL,
    last_error      VARCHAR(500) NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_outbox_pending ON outbox (status, next_attempt_at);