package com.mustycodified.musty_inventory_service.routes;

import com.mustycodified.musty_inventory_service.services.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventoryRouteBuilder extends RouteBuilder {

    private final InventoryReservationService inventoryReservationService;

    @Override
    public void configure() {
        getContext().setStreamCaching(false);
        configureErrorHandling();
        configureRoutes();
    }

    private void configureErrorHandling() {
        onException(Exception.class)
                .handled(true)
                .removeHeaders("*", "messageID")
                .process("errorHandlingProcessor")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .end();
    }

    private void configureRoutes() {
        // ── Reserve stock (or decline) for an order; downstream Kafka events are published via
        //    the outbox relay, not directly from this route — see InventoryReservationService.
        from("kafka:INVENTORY_RESERVE_TOPIC")
                .routeId("inventory-reservation-consumer")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .log(LoggingLevel.INFO, "Kafka Inbound Message. Body : ${body}")
                .bean(inventoryReservationService, "reserve")
                .log(LoggingLevel.INFO, "Reservation outcome: ${body}");
    }
}
