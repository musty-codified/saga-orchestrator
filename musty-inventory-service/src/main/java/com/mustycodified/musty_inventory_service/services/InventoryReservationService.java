package com.mustycodified.musty_inventory_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Reserves (or declines) stock for an order inside one local MySQL transaction, and writes the
 * resulting downstream event (PAYMENT_REQUEST_TOPIC on success, INVENTORY_DECLINED_TOPIC on
 * decline) to the outbox table atomically with that reservation change, instead of publishing to
 * Kafka directly from the route. {@link OutboxRelayService} relays outbox rows to Kafka separately.
 * - see PaymentRouteBuilder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationService {

    private static final String CONSUMER_ROUTE = "inventory-reservation-consumer";

    @Qualifier("mySqlJdbcTemplate")
    private final JdbcTemplate mySqlJdbcTemplate;
    private final TransactionTemplate mySqlTransactionTemplate;
    private final ObjectMapper objectMapper;

    public ReservationOutcome reserve(Map<String, Object> order) {
        String orderId = order.get("orderId").toString();
        int userId = Integer.parseInt(order.get("userId").toString());
        long variantProductId = Long.parseLong(order.get("variantProductId").toString());
        int quantity = Integer.parseInt(order.get("quantity").toString());
        BigDecimal totalPrice = new BigDecimal(order.get("totalPrice").toString());

        return mySqlTransactionTemplate.execute(status -> {
            try {
                mySqlJdbcTemplate.update(
                        "INSERT INTO inbox (consumer_route, dedup_key) VALUES (?, ?)",
                        CONSUMER_ROUTE, orderId);
            } catch (DuplicateKeyException e) {
                status.setRollbackOnly();
                log.info("Duplicate INVENTORY_RESERVE_TOPIC delivery — already processed → orderId: {}", orderId);
                return ReservationOutcome.DUPLICATE;
            }

            List<Map<String, Object>> product = mySqlJdbcTemplate.queryForList(
                    "SELECT id, status FROM variant_product WHERE id = ?", variantProductId);

            if (product.isEmpty() || !"ACTIVE".equalsIgnoreCase(String.valueOf(product.get(0).get("status")))) {
                log.warn("Product not found or inactive — declining → orderId: {}, productId: {}", orderId, variantProductId);
                writeOutboxEvent(orderId, "INVENTORY_DECLINED", "INVENTORY_DECLINED_TOPIC",
                        Map.of("orderId", orderId, "status", "DECLINED", "reason", "PRODUCT_NOT_FOUND"));
                return ReservationOutcome.DECLINED;
            }
            List<Map<String, Object>> stockRow = mySqlJdbcTemplate.queryForList(
                    "SELECT quantity FROM stocks WHERE variant_product_id = ? FOR UPDATE", variantProductId);

            int availableQuantity = stockRow.isEmpty() ? 0 : ((Number) stockRow.get(0).get("quantity")).intValue();

            if (availableQuantity < quantity) {
                log.warn("Insufficient stock — declining → orderId: {}, requested: {}, available: {}",
                        orderId, quantity, availableQuantity);
                writeOutboxEvent(orderId, "INVENTORY_DECLINED", "INVENTORY_DECLINED_TOPIC",
                        Map.of("orderId", orderId, "status", "DECLINED", "reason", "OUT_OF_STOCK"));
                return ReservationOutcome.DECLINED;
            }

            int rowsUpdated = mySqlJdbcTemplate.update(
                    "UPDATE stocks SET quantity = quantity - ? WHERE variant_product_id = ? AND quantity >= ?",
                    quantity, variantProductId, quantity);

            if (rowsUpdated == 0) {
                log.warn("Lost race on stock decrement — declining → orderId: {}, productId: {}", orderId, variantProductId);
                writeOutboxEvent(orderId, "INVENTORY_DECLINED", "INVENTORY_DECLINED_TOPIC",
                        Map.of("orderId", orderId, "status", "DECLINED", "reason", "OUT_OF_STOCK"));
                return ReservationOutcome.DECLINED;
            }

            mySqlJdbcTemplate.update(
                    "INSERT INTO stock_reservations (order_id, variant_product_id, quantity_reserved, status) " +
                            "VALUES (?, ?, ?, 'RESERVED')",
                    orderId, variantProductId, quantity);

            writeOutboxEvent(orderId, "INVENTORY_RESERVED", "PAYMENT_REQUEST_TOPIC",
                    Map.of("orderId", orderId, "userId", userId, "totalPrice", totalPrice));

            log.info("Stock reserved → orderId: {}, productId: {}, quantity: {}", orderId, variantProductId, quantity);
            return ReservationOutcome.RESERVED;
        });
    }

    private void writeOutboxEvent(String orderId, String eventType, String topic, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            mySqlJdbcTemplate.update(
                    "INSERT INTO outbox (aggregate_type, aggregate_id, event_type, topic, payload) " +
                            "VALUES ('STOCK_RESERVATION', ?, ?, ?, ?)",
                    orderId, eventType, topic, json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload for orderId " + orderId, e);
        }
    }
}
