package com.mustycodified.musty_process_payment.routes;

import com.mustycodified.commonlib.models.outbox.OutboxRow;
import com.mustycodified.musty_process_payment.services.PaymentOutboxRelayService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Polls the local {@code payments_outbox} table and republishes pending rows to Kafka. Own
 * RouteBuilder so its per-row doTry/doCatch failure handling doesn't interact with
 * AppRouteBuilder's blanket onException(Exception.class) handler.
 */
@Component
@RequiredArgsConstructor
public class PaymentOutboxRelayRouteBuilder extends RouteBuilder {

    private final PaymentOutboxRelayService paymentOutboxRelayService;

    @Override
    public void configure() {
        from("timer://paymentsOutboxRelay?period=2000")
                .routeId("payments-outbox-relay")
                .bean(paymentOutboxRelayService, "fetchPending")
                .split(body())
                    .process(exchange -> exchange.setProperty("outboxRow", exchange.getIn().getBody(OutboxRow.class)))
                    .setHeader("kafkaTopic", simple("${body.topic}"))
                    .setBody(simple("${body.payload}"))
                    .doTry()
                        .toD("kafka:${header.kafkaTopic}?brokers={{spring.kafka.bootstrap-servers}}")
                        .process(exchange -> paymentOutboxRelayService.markPublished(
                                exchange.getProperty("outboxRow", OutboxRow.class)))
                        .log(LoggingLevel.DEBUG, "Relayed payments outbox row → topic: ${header.kafkaTopic}")
                    .doCatch(Exception.class)
                        .process(exchange -> paymentOutboxRelayService.markFailedAttempt(
                                exchange.getProperty("outboxRow", OutboxRow.class),
                                exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class)))
                    .end()
                .end();
    }
}
