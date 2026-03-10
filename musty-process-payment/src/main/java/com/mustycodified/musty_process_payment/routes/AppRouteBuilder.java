package com.mustycodified.musty_process_payment.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppRouteBuilder extends RouteBuilder {
    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBrokerUrl;

    @Override
    public void configure() throws Exception {
        getContext().setStreamCaching(false);
        configureErrorHandling();
        configureRest();
    }

    private void configureErrorHandling() {
        onException(Exception.class)
                .handled(true)
                .removeHeaders("*", "messageID")
                .process("errorHandlingProcessor")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .end();

        onException(PSQLException.class)
                .handled(true)
                .process(exchange -> {
                    PSQLException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, PSQLException.class);
                    int statusCode = exception.getErrorCode();
                    String errorMessage = exception.getMessage();
                    exchange.getIn().setBody(errorMessage);
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
                })
                .log("Exception while inserting messages.");

    }

    private void configureRest() {
        // Simulate Successful Payment
        from("direct:payment-success")
                .log(LoggingLevel.INFO, "Processing payment...")
                .process(exchange -> {
                    String transactionId = UUID.randomUUID().toString();
                    exchange.getIn().setBody(Map.of(
                            "status", "SUCCESS",
                            "transactionId", transactionId
                    ));
                });

        // Simulate Payment Failure
        from("direct:payment-failure")
                .log(LoggingLevel.INFO, "Simulating Payment failure!")
                .process(exchange -> {
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                    exchange.getIn().setBody(Map.of(
                            "status", "FAILED",
                            "error", "Insufficient funds"
                    ));
                });

        // Simulate Payment Gateway Timeout
        from("direct:payment-timeout")
                .log(LoggingLevel.INFO, "Simulating timeout...")
                .delay(10000)
                .process(exchange -> {
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 504);
                    exchange.getIn().setBody(Map.of(
                            "status", "TIMEOUT",
                            "error", "Payment gateway downtime"
                    ));
                });

        from("kafka:PAYMENT_REQUEST_TOPIC?brokers="+kafkaBrokerUrl+"&groupId=payment-service")
                .routeId("PaymentInitiatedEvent")
                .process(exchange -> {
                    UUID paymentReference = UUID.randomUUID();
                    exchange.getIn().setHeader("payment_reference", paymentReference);
                    log.info("Generated payment reference: {}", paymentReference);
                    String orderId = exchange.getIn().getHeader("orderId", String.class);
                    String userId = exchange.getIn().getHeader("userId", String.class);
                    String amountStr = exchange.getIn().getHeader("totalPrice", String.class);
                    Integer amount = (amountStr != null && !amountStr.isEmpty()) ? Integer.parseInt(amountStr) : 0;

                    // Set headers Before SQL insertion
                    exchange.getIn().setHeader("payment_reference", paymentReference);
                    exchange.getIn().setHeader("orderId", orderId);
                    exchange.getIn().setHeader("userId", userId);
                    exchange.getIn().setHeader("amount", amount);
                })
                .log(LoggingLevel.INFO, "Logging transaction in DB for order → OrderID: ${header.orderId}")

                .to("sql:INSERT INTO payments (payment_reference, order_id, user_id, amount, status) " +
                        "VALUES (CAST(:#payment_reference AS UUID), CAST(:#orderId AS UUID), :#userId, :#amount, 'PENDING')?dataSource=#postgresDataSource")

                .log(LoggingLevel.INFO, "Calling Mock Payment Gateway")
                .to("direct:payment-success")

                .log(LoggingLevel.INFO, "Update payment status in database")
                .to("sql:UPDATE payments SET status='SUCCESS' WHERE payment_reference=:#payment_reference")
                .log("Raw message body: ${body}")
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, "Emit payment status event")
                .to("kafka:PAYMENT_STATUS_TOPIC?brokers=" + kafkaBrokerUrl);

    }


}

