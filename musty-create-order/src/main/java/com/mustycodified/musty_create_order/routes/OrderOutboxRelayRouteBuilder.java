package com.mustycodified.musty_create_order.routes;

import com.mustycodified.commonlib.models.outbox.OutboxRow;
import com.mustycodified.musty_create_order.services.OrderOutboxRelayService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Polls the local {@code outbox} table and republishes pending rows to Kafka. Own RouteBuilder so
 * its per-row doTry/doCatch failure handling doesn't interact with OrderRouteBuilder's blanket
 * onException(Exception.class) handler.  — see InventoryRouteBuilder.
 */
@Component
@RequiredArgsConstructor
public class OrderOutboxRelayRouteBuilder extends RouteBuilder {

    private final OrderOutboxRelayService orderOutboxRelayService;

    @Override
    public void configure() {
        from("timer://ordersOutboxRelay?period=2000")
                .routeId("orders-outbox-relay")
                .bean(orderOutboxRelayService, "fetchPending")
                .split(body())
                    .process(exchange -> exchange.setProperty("outboxRow", exchange.getIn().getBody(OutboxRow.class)))
                    .setHeader("kafkaTopic", simple("${body.topic}"))
                    .setBody(simple("${body.payload}"))
                    .doTry()
                .toD("kafka:${header.kafkaTopic}?brokers={{spring.kafka.bootstrap-servers}}")
                        .process(exchange -> orderOutboxRelayService.markPublished(
                                exchange.getProperty("outboxRow", OutboxRow.class)))
                        .log(LoggingLevel.DEBUG, "Relayed orders outbox row → topic: ${header.kafkaTopic}")
                    .doCatch(Exception.class)
                        .process(exchange -> orderOutboxRelayService.markFailedAttempt(
                                exchange.getProperty("outboxRow", OutboxRow.class),
                                exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class)))
                    .end()
                .end();
    }
}