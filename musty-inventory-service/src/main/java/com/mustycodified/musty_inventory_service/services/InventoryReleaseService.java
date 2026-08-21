package com.mustycodified.musty_inventory_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * The second compensating transaction: when payment fails, release the stock reservation held
 * for that order so it becomes sellable again. This is the compensating counterpart to
 * {@link InventoryReservationService#reserve}. The release and its outbound
 * INVENTORY_RELEASED_TOPIC event are written atomically to the same local transaction via the
 * outbox table, so the compensation can't be silently lost to a mid-flight crash.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReleaseService {

    private static final String CONSUMER_ROUTE = "InventoryReleaseEvent";

    @Qualifier("mySqlJdbcTemplate")
    private final JdbcTemplate mySqlJdbcTemplate;
    private final TransactionTemplate mySqlTransactionTemplate;
    private final ObjectMapper objectMapper;

    public ReleaseOutcome release(String orderId) {
        return mySqlTransactionTemplate.execute(status -> {
            try {
                mySqlJdbcTemplate.update(
                        "INSERT INTO inbox (consumer_route, dedup_key) VALUES (?, ?)",
                        CONSUMER_ROUTE, orderId);
            } catch (DuplicateKeyException e) {
                status.setRollbackOnly();
                log.info("Duplicate PAYMENT_FAILED_TOPIC delivery — release already processed → orderId: {}", orderId);
                return ReleaseOutcome.DUPLICATE;
            }

            List<Map<String, Object>> reservations = mySqlJdbcTemplate.queryForList(
                    "SELECT variant_product_id, quantity_reserved, status FROM stock_reservations " +
                            "WHERE order_id = ? FOR UPDATE", orderId);

            if (reservations.isEmpty()) {
                log.warn("No reservation found to release — order was never reserved (e.g. already declined) → orderId: {}", orderId);
                return ReleaseOutcome.NOT_FOUND;
            }

            Map<String, Object> reservation = reservations.get(0);
            String reservationStatus = String.valueOf(reservation.get("status"));

            if (!"RESERVED".equals(reservationStatus)) {
                log.info("Reservation already {} — idempotent no-op → orderId: {}", reservationStatus, orderId);
                return ReleaseOutcome.ALREADY_RELEASED;
            }

            long variantProductId = ((Number) reservation.get("variant_product_id")).longValue();
            int quantityReserved = ((Number) reservation.get("quantity_reserved")).intValue();

            mySqlJdbcTemplate.update(
                    "UPDATE stocks SET quantity = quantity + ? WHERE variant_product_id = ?",
                    quantityReserved, variantProductId);

            mySqlJdbcTemplate.update(
                    "UPDATE stock_reservations SET status = 'RELEASED' WHERE order_id = ?", orderId);

            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "orderId", orderId,
                        "variantProductId", variantProductId,
                        "quantityReleased", quantityReserved
                ));
                mySqlJdbcTemplate.update(
                        "INSERT INTO outbox (aggregate_type, aggregate_id, event_type, topic, payload) " +
                                "VALUES ('STOCK_RESERVATION', ?, 'INVENTORY_RELEASED', 'INVENTORY_RELEASED_TOPIC', ?)",
                        orderId, json);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize INVENTORY_RELEASED payload for orderId " + orderId, e);
            }

            log.warn("Stock released — compensating for payment failure → orderId: {}, productId: {}, quantity: {}",
                    orderId, variantProductId, quantityReserved);
            return ReleaseOutcome.RELEASED;
        });
    }
}
