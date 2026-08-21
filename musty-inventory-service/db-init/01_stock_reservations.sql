-- Tracks the reservation lifecycle for each order's stock hold, decoupling "available to sell"
-- (stocks.quantity) from a specific order so a failed payment has something concrete to release.
CREATE TABLE IF NOT EXISTS stock_reservations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            CHAR(36)     NOT NULL,
    variant_product_id  BIGINT       NOT NULL,
    quantity_reserved   INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'RESERVED',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_stock_reservations_order_id UNIQUE (order_id)
);
