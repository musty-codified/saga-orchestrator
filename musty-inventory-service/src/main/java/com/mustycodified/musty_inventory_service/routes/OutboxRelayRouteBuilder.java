package com.mustycodified.musty_inventory_service.routes;

import com.mustycodified.commonlib.models.outbox.OutboxRow;
import com.mustycodified.musty_inventory_service.services.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Polls the local {@code outbox} table and republishes pending rows to Kafka. Kept as its own
 * RouteBuilder (separate from AppRouteBuilder) so its per-row doTry/doCatch failure handling
 * doesn't interact with AppRouteBuilder's blanket onException(Exception.class) handler.
 */
@Component
@RequiredArgsConstructor
public class OutboxRelayRouteBuilder extends RouteBuilder {

    private final OutboxRelayService outboxRelayService;

    @Override
    public void configure() {
        from("timer://outboxRelay?period=2000")
                .routeId("inventory-outbox-relay")
                .bean(outboxRelayService, "fetchPending")
                .split(body())
                    .process(exchange -> exchange.setProperty("outboxRow", exchange.getIn().getBody(OutboxRow.class)))
                    .setHeader("kafkaTopic", simple("${body.topic}"))
                    .setBody(simple("${body.payload}"))
                    .doTry()
                 .toD("kafka:${header.kafkaTopic}?brokers={{spring.kafka.bootstrap-servers}}")
                        .process(exchange -> outboxRelayService.markPublished(
                                exchange.getProperty("outboxRow", OutboxRow.class)))
                        .log(LoggingLevel.DEBUG, "Relayed outbox row → topic: ${header.kafkaTopic}")
                    .doCatch(Exception.class)
                        .process(exchange -> outboxRelayService.markFailedAttempt(
                                exchange.getProperty("outboxRow", OutboxRow.class),
                                exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class)))
                    .end()
                .end();
    }
    ////            INVENTORY_DECLINED_TOPIC
    ////            PAYMENT_REQUEST_TOPIC
}
