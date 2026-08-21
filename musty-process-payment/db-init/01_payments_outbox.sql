-- Transactional outbox for musty-process-payment: PAYMENT_FAILED_TOPIC (the trigger for the
-- inventory-release compensating transaction) is written here atomically with the payments
-- status update, instead of being published as a separate, crash-vulnerable step.
CREATE TABLE IF NOT EXISTS payments_outbox (
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

CREATE INDEX IF NOT EXISTS idx_payments_outbox_pending ON payments_outbox (status, next_attempt_at);
