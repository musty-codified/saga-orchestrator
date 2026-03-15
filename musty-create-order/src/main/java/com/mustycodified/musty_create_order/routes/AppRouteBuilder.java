package com.mustycodified.musty_create_order.routes;

import com.mustycodified.musty_create_order.commonlib.models.OrderResponse;
import com.mustycodified.musty_create_order.commonlib.models.RequestPayload;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
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
                    .maximumRedeliveries(3)
                    .redeliveryDelay(1000)
                     .logRetryAttempted(true)
                      .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
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
        restConfiguration()
                .component("servlet")
                .host("0.0.0.0")
                .port(serverPort)
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .corsAllowCredentials(true)
                .enableCORS(true);

        rest("/orders/{userId}/{variantProductId}")
                .post()
                .param().name("userId").type(RestParamType.path)
                .description("user id")
                .endParam()
                .param().name("variantProductId").type(RestParamType.path)
                .description("ID of product to order")
                .required(true).endParam()
                .param().name("body").type(RestParamType.body)
                .description("Order DTO")
                .required(true).endParam()
                .consumes(APPLICATION_JSON_VALUE)
                .produces(APPLICATION_JSON_VALUE)
                .type(RequestPayload.class)
                .outType(OrderResponse.class)
                .responseMessage().code(CREATED.value())
                .message("Order created successfully")
                .endResponseMessage()
                .to("direct:createOrder");

        from("direct:createOrder")
                .routeId("order-service-route")
                .log(LoggingLevel.INFO, "New Order Received → UserID: ${header.userId}, ProductID: ${header.variantProductId}")
                .process("orderCreationProcessor")
                .process(exchange -> {
                    UUID orderId = UUID.randomUUID();
                    exchange.getIn().setHeader("orderId", orderId);

                })
                .process("kafkaMessageBuilderProcessor")
                .log(LoggingLevel.INFO, "Insert order into Postgres")
                .to("sql:INSERT INTO orders (order_id, user_id, product_id, quantity, total_price, status) " +
                        "VALUES (:#orderId::uuid, :#userId, :#variantProductId, :#quantity, :#totalPrice, 'PENDING')?dataSource=#postgresDataSource&outputType=SelectList")

                //marshall the message into JSON before sending over the network
                .marshal().json(JsonLibrary.Jackson)

                // Send order message to inventory check topic
                .to("kafka:INVENTORY_CHECK_TOPIC?brokers={{spring.kafka.bootstrap-servers}}")

                .log(LoggingLevel.INFO, "Return JSON message immediately")
                .process("orderResponseDTOProcessor")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202));

        from("kafka:INVENTORY_CHECK_TOPIC?brokers={{spring.kafka.bootstrap-servers}}&groupId=inventory-service")
                .routeId("inventory-service-route")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(exchange -> {
                    Map<String, Object> message = exchange.getIn().getBody(Map.class);
                    exchange.getIn().setHeader("orderId", message.get("orderId"));
                    exchange.getIn().setHeader("userId", message.get("userId"));
                    exchange.getIn().setHeader("variantProductId", message.get("variantProductId"));
                    exchange.getIn().setHeader("totalPrice", message.get("totalPrice"));
                    exchange.getIn().setHeader("quantity", message.get("quantity"));
                    log.info("Restored headers → orderId={}, variantProductId={}", message.get("orderId"), message.get("variantProductId"));
                })
                .log(LoggingLevel.INFO, "Query product from primary MySQL →: ${header.variantProductId}")
                .to("sql:SELECT vp.id, vp.name, vp.price, vp.status, s.quantity " +
                        "FROM variant_product vp " +
                        "LEFT JOIN stocks s ON vp.id = s.variant_product_id " +
                        "WHERE vp.id = :#variantProductId?dataSource=#mySqlDataSource&outputType=SelectList")
                .process("checkProductProcessor")
                .log(LoggingLevel.INFO, "Product Exists: ${header.productExists}, Quantity: ${exchangeProperty.quantity}")
                .choice()
                .when(simple("${exchangeProperty.quantity} > 0"))
                .log(LoggingLevel.INFO, "Product in stock! Trigger payment request...")
                .process(exchange -> {
                    String orderId = exchange.getIn().getHeader("orderId", String.class);
                    String userId = exchange.getIn().getHeader("userId", String.class);
                    BigDecimal amountStr = exchange.getIn().getHeader("totalPrice", BigDecimal.class);

                    // Set headers Before publishing Kafka Message to Payment Service
                    exchange.getIn().setHeader("orderId", orderId);
                    exchange.getIn().setHeader("userId", userId);
                    exchange.getIn().setHeader("totalPrice", amountStr);
                    log.info("Emitting Payment event for → orderId: {}, userId: {}, amount: {}", orderId, userId, amountStr);
                })
                .to("kafka:PAYMENT_REQUEST_TOPIC?brokers={{spring.kafka.bootstrap-servers}}")
                .otherwise()
                .log(LoggingLevel.ERROR, "Product out of stock. Declining order...")
                .to("sql:UPDATE orders SET status='DECLINED' WHERE order_id = :#orderId?dataSource=#postgresDataSource")
                .log(LoggingLevel.INFO, "Order DECLINED.");

        from("kafka:PAYMENT_STATUS_TOPIC?brokers={{spring.kafka.bootstrap-servers}}&groupId=order-service")
                .routeId("PaymentStatusUpdatedEvent")
                .log(LoggingLevel.INFO, "Updating order status → Order ID: ${header.orderId}")
                .process(exchange -> {
                 String orderIdStr = exchange.getIn().getHeader("orderId", String.class);
                    exchange.getIn().setHeader("orderId", orderIdStr);
                    log.info("Updating order status in postgres → orderId: {}", orderIdStr);
                })
                // Update Order Status in Database
                .to("sql:UPDATE orders SET status='PAID' WHERE order_id = :#orderId?datasource=#postgresDataSource")
                .log(LoggingLevel.INFO, "Order status updated to PAID.");
    }

}


