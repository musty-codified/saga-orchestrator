package com.mustycodified.musty_notification_service.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppRouteBuilder extends RouteBuilder {
    @Value("${server.port:8080}")
    private int serverPort;

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

        from("kafka:PAYMENT_STATUS_TOPIC")
                .routeId("NotificationRequestedEvent")
                .log(LoggingLevel.INFO, "Sending payment confirmation for OrderID: ${header.orderId}")
                .process(exchange -> {
                    String orderId = exchange.getIn().getHeader("orderId", String.class);
                    exchange.getIn().setHeader("orderId", orderId);
                })
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                // Insert notification logs in Mongo
                .to("mongodb:mongoBean?database=notifications&collection=events&operation=insert")
                // Notify user by calling email service
                .to("direct:notification-success")
                // Send order to ship service
                .to("kafka:ORDER_SHIPPING_TOPIC");

        //Simulate sending notification
        from("direct:notification-success")
                .routeId("mock-notification-endpoint")
                .log(LoggingLevel.INFO, "Notification Sent → OrderID: ${header.orderId}, UserID: ${header.userId}")
                .setBody().constant(Map.of("message", "Notification Sent", "status", "SUCCESS"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));
    }

}


