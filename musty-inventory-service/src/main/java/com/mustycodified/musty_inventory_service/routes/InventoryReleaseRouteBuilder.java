package com.mustycodified.musty_inventory_service.routes;

import com.mustycodified.musty_inventory_service.services.InventoryReleaseService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Second compensating transaction: consumes PAYMENT_FAILED_TOPIC and releases the stock
 * reservation held for the order, restocking it. Its own RouteBuilder (and own errorHandler) so
 * the retry/DLQ policy for this compensating action is independent of the outbox relay's.
 */
@Component
@RequiredArgsConstructor
public class InventoryReleaseRouteBuilder extends RouteBuilder {

    private final InventoryReleaseService inventoryReleaseService;

    @Override
    public void configure() {
        errorHandler(deadLetterChannel("kafka:INVENTORY_RELEASE_DLQ_TOPIC?brokers={{spring.kafka.bootstrap-servers}}")
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        from("kafka:PAYMENT_FAILED_TOPIC?groupId=inventory-service-release-group")
                .routeId("InventoryReleaseEvent")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .choice()
                    .when(simple("${body[status]} == 'FAILED'"))
                        .log(LoggingLevel.WARN, "Compensation triggered — releasing reserved stock → OrderID: ${body[orderId]}")
                        .process(exchange -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> body = exchange.getIn().getBody(Map.class);
                            exchange.getIn().setHeader("orderId", body.get("orderId").toString());
                        })
                        .process(exchange -> inventoryReleaseService.release(exchange.getIn().getHeader("orderId", String.class)))
                        .log(LoggingLevel.INFO, "Stock release processed → OrderID: ${header.orderId}")
                .end();
    }
}
