package com.mustycodified.musty_process_payment.routes;

import com.mustycodified.musty_process_payment.services.PaymentFailureService;
import com.mustycodified.musty_process_payment.services.PaymentSuccessService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.postgresql.util.PSQLException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PaymentRouteBuilder extends RouteBuilder {

    private final PaymentFailureService paymentFailureService;

    @Override
    public void configure() throws Exception {
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

        onException(PSQLException.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .logRetryAttempted(true)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .handled(true)
                .process(exchange -> {
                    PSQLException ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, PSQLException.class);
                    exchange.getIn().setBody(ex.getMessage());
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, ex.getErrorCode());
                    log.warn("PSQLException in payment service: {}", ex.getMessage());
                })
                .end();
    }

    private void configureRoutes() {
        // ── Payment processing ────────────────────────────────────────────────
        from("kafka:PAYMENT_REQUEST_TOPIC")
                .routeId("payment-processing-consumer")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .log(LoggingLevel.INFO, "Kafka Inbound Message. Body : ${body}")
                .doTry()
                .process(exchange -> {
                    Map<String, Object> body = exchange.getIn().getBody(Map.class);

                    String orderId = body.get("orderId").toString();
                    int userId = Integer.parseInt(body.get("userId").toString());
                    BigDecimal amount = new BigDecimal(body.get("totalPrice").toString());

                    // Generate and store tnx reference in headers
                    String paymentRef = UUID.randomUUID().toString();

                    exchange.getIn().setHeader("payment_reference", paymentRef);
                    exchange.getIn().setHeader("orderId", orderId);
                    exchange.getIn().setHeader("userId", userId);
                    exchange.getIn().setHeader("amount", amount);

                })
                .process(exchange -> exchange.getIn().setBody(Map.of(
                        "payment_reference", exchange.getIn().getHeader("payment_reference"),
                        "orderId",           exchange.getIn().getHeader("orderId"),
                        "userId",            exchange.getIn().getHeader("userId"),
                        "amount",            exchange.getIn().getHeader("amount")
                )))
                .log(LoggingLevel.INFO, "Initiating payment for OrderID: ${header.orderId}")
                .to("sql:INSERT INTO payments (payment_reference, order_id, user_id, amount, payment_status) " +
                        "VALUES (CAST(:#payment_reference AS UUID), CAST(:#orderId AS UUID), :#userId, :#amount, 'PENDING')" +
                        "?dataSource=#postgresDataSource")

                // ── Build Paystack init request ─────────────────────────────────
                .process(exchange -> {
                    BigDecimal amount = exchange.getIn().getHeader("amount", BigDecimal.class);
                    long amountInKobo = amount.movePointRight(2).longValueExact(); // Paystack wants kobo, not naira
                    exchange.getIn().setBody(Map.of(
                            "reference", exchange.getIn().getHeader("payment_reference"),
                            "amount", amountInKobo,
                            "email", exchange.getIn().getHeader("customerEmail", "guest@musty.dev", String.class),
                            "callback_url", "{{app.base-url}}/payments/callback"
                    ));
                })
                .marshal().json(JsonLibrary.Jackson)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Authorization", simple("Bearer {{paystack.secret-key}}"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("https://api.paystack.co/transaction/initialize")
                .log(LoggingLevel.INFO, "Paystack initialize response: ${body}")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Payment initiation failed for order ${header.orderId}: ${exception.message}")
                .process(exchange -> paymentFailureService.recordFailure(
                        exchange.getIn().getHeader("payment_reference", "", String.class),
                        exchange.getIn().getHeader("orderId", "", String.class)))
                .end();

                //=================================Calling mock payment gateway===============================//
//                .to("direct:payment-success-path")
//                .process(exchange -> {
//                    Map<?, ?> gatewayResponse = exchange.getIn().getBody(Map.class);
//                    if (gatewayResponse != null && "FAILED".equals(gatewayResponse.get("status"))) {
//                        throw new RuntimeException("Payment gateway rejected: " + gatewayResponse.get("error"));
//                    }
//                })
//                .process(exchange -> exchange.getIn().setBody(Map.of(
//                        "payment_reference", exchange.getIn().getHeader("payment_reference")
//                )))
//                .process(exchange -> paymentSuccessService.recordSuccess(
//                        exchange.getIn().getHeader("payment_reference", "", String.class),
//                        exchange.getIn().getHeader("orderId", "", String.class)))
//                // Build Kafka body — order service reads orderId from body after unmarshal
//                .process(exchange -> exchange.getIn().setBody(Map.of(
//                        "orderId", exchange.getIn().getHeader("orderId"),
//                        "status",  "SUCCESS"
//                )))
//                .marshal().json(JsonLibrary.Jackson)
//                .log(LoggingLevel.INFO, "Publishing PAYMENT_SUCCESS_TOPIC → OrderID: ${header.orderId}")
//                //    see OrderRouteBuilder.
//                .to("kafka:PAYMENT_SUCCESS_TOPIC?brokers={{spring.kafka.bootstrap-servers}}")
//                .doCatch(Exception.class)
//                .log(LoggingLevel.ERROR, "Payment failed for order ${header.orderId}: ${exception.message}")
//                .process(exchange -> paymentFailureService.recordFailure(
//                        exchange.getIn().getHeader("payment_reference", "", String.class),
//                        exchange.getIn().getHeader("orderId", "", String.class)))
//                .end();

        // ── Mock payment gateway endpoints ────────────────────────────────────
//        from("direct:payment-success")
//                .routeId("mock-payment-success")
//                .log(LoggingLevel.INFO, "Mock gateway: processing payment...")
//                .process(exchange -> exchange.getIn().setBody(Map.of(
//                        "status", "SUCCESS",
//                        "transactionId", UUID.randomUUID().toString()
//                )));
//
//        from("direct:payment-failure")
//                .routeId("mock-payment-failure")
//                .log(LoggingLevel.WARN, "Mock gateway: simulating failure")
//                .process(exchange -> exchange.getIn().setBody(Map.of(
//                        "status", "FAILED",
//                        "error", "Insufficient funds"
//                )));
//
//        from("direct:payment-timeout")
//                .routeId("mock-payment-timeout")
//                .delay(10000)
//                .process(exchange -> exchange.getIn().setBody(Map.of(
//                        "status", "TIMEOUT",
//                        "error", "Payment gateway downtime"
//                )));
    }
}