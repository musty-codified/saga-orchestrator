package com.mustycodified.musty_create_order.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Persists a new order and its outbound INVENTORY_RESERVE_TOPIC event atomically, via the same
 * outbox mechanism used in musty-inventory-service/musty-process-payment. Previously these were
 * two independent steps (SQL insert, then a direct Kafka publish) — a crash between them left the
 * order stuck in PENDING forever with no inventory-reserve event ever published, and nothing
 * downstream to compensate it since inventory-service never even learned the order existed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCreationService {

    private static final String RESERVE_TOPIC = "INVENTORY_RESERVE_TOPIC";

    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate postgresJdbcTemplate;
    private final TransactionTemplate postgresTransactionTemplate;
    private final ObjectMapper objectMapper;

    public void createOrder(UUID orderId, int userId, int variantProductId, int quantity, BigDecimal totalPrice) {
        postgresTransactionTemplate.executeWithoutResult(status -> {
            postgresJdbcTemplate.update(
                    "INSERT INTO orders (order_id, user_id, product_id, quantity, total_price, order_status) " +
                            "VALUES (CAST(? AS UUID), ?, ?, ?, ?, 'INITIATED')",
                    orderId.toString(), userId, variantProductId, quantity, totalPrice);

            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "orderId", orderId.toString(),
                        "userId", userId,
                        "variantProductId", variantProductId,
                        "quantity", quantity,
                        "totalPrice", totalPrice
                ));
                postgresJdbcTemplate.update(
                        "INSERT INTO outbox (aggregate_type, aggregate_id, event_type, topic, payload) " +
                                "VALUES ('ORDER', ?, 'ORDER_CREATED', ?, ?::jsonb)",
                        orderId.toString(), RESERVE_TOPIC, json);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize INVENTORY_RESERVE_TOPIC payload for orderId " + orderId, e);
            }

            log.info("Order INITIATED, reserve event queued → orderId: {}, productId: {}, quantity: {}",
                    orderId, variantProductId, quantity);
        });
    }
}