package com.mustycodified.musty_create_order.routes;

import com.mustycodified.musty_create_order.commonlib.models.OrderResponse;
import com.mustycodified.musty_create_order.commonlib.models.RequestPayload;
import com.mustycodified.musty_create_order.processors.OrderResponseDTOProcessor;
import com.mustycodified.musty_create_order.services.OrderCreationService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class OrderRouteBuilder extends RouteBuilder {

    @Value("${server.port:8080}")
    private int serverPort;

    private static final Logger log = LoggerFactory.getLogger(OrderRouteBuilder.class);
    private final OrderCreationService orderCreationService;

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
                    log.warn("PSQLException: {}", ex.getMessage());
                })
                .log("Database error: ${}");
    }

    private void configureRest() {
        restConfiguration()
                .component("servlet")
                .host("0.0.0.0")
                .port(serverPort)
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .corsAllowCredentials(true)
                .enableCORS(true)
                .apiContextPath("/openapi.json")
                .apiProperty("api.title", "Order Service API")
                .apiProperty("api.version", "1.0.0");


        // ── REST endpoint ─────────────────────────────────────────────────────
        rest("/orders/{userId}/{variantProductId}")
                .post()
                .param().name("userId").type(RestParamType.path).description("user ID").endParam()
                .param().name("variantProductId").type(RestParamType.path)
                    .description("product ID").required(true).endParam()
                .param().name("body").type(RestParamType.body)
                    .description("Message body").required(true).endParam()
                .consumes(APPLICATION_JSON_VALUE)
                .produces(APPLICATION_JSON_VALUE)
                .type(RequestPayload.class)
                .outType(OrderResponse.class)
                .responseMessage().code(ACCEPTED.value()).message("Transaction initiated").endResponseMessage()
                .to("direct:create-order");

        // ── Create Order ──────────────────────────────────────────────────────
        from("direct:create-order")
                .routeId("order-create")
                .process("orderMessageBuilderProcessor")
                .log(LoggingLevel.INFO, "UserID: ${header.userId}, ProductID: ${header.variantProductId}")
                .process(exchange -> orderCreationService.createOrder(
                        exchange.getIn().getHeader("orderId", UUID.class),
                        exchange.getIn().getHeader("userId", int.class),
                        exchange.getIn().getHeader("variantProductId", int.class),
                        exchange.getIn().getHeader("quantity", Integer.class),
                        exchange.getIn().getHeader("totalPrice", java.math.BigDecimal.class)))
                .process(new OrderResponseDTOProcessor())
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202));

        // ── Payment confirmed: mark order PAID ───────────────────────────────
        from("kafka:PAYMENT_SUCCESS_TOPIC")
                .routeId("payment-updated-route")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .log(LoggingLevel.INFO, "kafka inbound payload: ${body}")
                .process(exchange -> {
                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
                    UUID orderId = UUID.fromString(body.get("orderId").toString());
                    exchange.getIn().setHeader("orderId", orderId);
                })

                .to("sql:UPDATE orders SET order_status='PAID' WHERE order_id = :#orderId::uuid?dataSource=#postgresDataSource")
                .log(LoggingLevel.INFO, "Order status updated to PAID → OrderID: ${header.orderId}");
        // ── Tentative end of Request lifecycle ──────────

        // ── Payment failed: compensating action → mark order FAILED ──────────
        from("kafka:PAYMENT_FAILED_TOPIC?groupId=order-service-payment-failed-group")
                .routeId("PaymentFailedCompensationEvent")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(exchange -> {
                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
                    UUID orderId = UUID.fromString(body.get("orderId").toString());
                    exchange.getIn().setHeader("orderId", orderId);
                })
                .to("sql:UPDATE orders SET order_status='FAILED' WHERE order_id = :#orderId::uuid?dataSource=#postgresDataSource")
                .log(LoggingLevel.WARN, "Order marked FAILED → OrderID: ${header.orderId}");

        // ── Inventory out of stock: mark order DECLINED ───────────────────────
        from("kafka:INVENTORY_DECLINED_TOPIC")
                .routeId("InventoryDeclinedEvent")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(exchange -> {
                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
                    UUID orderId = UUID.fromString(body.get("orderId").toString());
                    exchange.getIn().setHeader("orderId", orderId);
                })
                .to("sql:UPDATE orders SET order_status='DECLINED' WHERE order_id = :#orderId::uuid?dataSource=#postgresDataSource")
                .log(LoggingLevel.WARN, "Order marked DECLINED → OrderID: ${header.orderId}");
    }
}