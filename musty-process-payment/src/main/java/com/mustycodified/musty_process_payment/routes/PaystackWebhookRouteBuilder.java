package com.mustycodified.musty_process_payment.routes;

import com.mustycodified.commonlib.enums.HTTPCommonHeadersEnum;
import com.mustycodified.musty_process_payment.services.PaymentFailureService;
import com.mustycodified.musty_process_payment.services.PaymentSuccessService;
import com.mustycodified.musty_process_payment.services.WebhookInboxService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.TryDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.mustycodified.commonlib.enums.HTTPCommonHeadersEnum.PAYSTACK_VERIFICATION;

@Component
@RequiredArgsConstructor
public class PaystackWebhookRouteBuilder extends RouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(PaystackWebhookRouteBuilder.class);

    @Value("${server.port:8080}")
    private int serverPort;

    private final WebhookInboxService webhookInboxService;
    private final PaymentFailureService paymentFailureService;
    private final PaymentSuccessService paymentSuccessService;
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
                .enableCORS(true);

        // ── Clients poll `http://localhost:8083/api/v1/webhooks/paystack` for the final outcome.
        RouteDefinition route = from("servlet:///webhooks/paystack?servletName=PaymentWebhookServlet&httpMethodRestrict=POST")
                .routeId("paystack-webhook-event");

        TryDefinition tryDef = route.doTry();

        tryDef
                // ── Verify signature against the RAW body before trusting anything ──
                .process(exchange -> {
                    String rawBody = exchange.getIn().getBody(String.class);
                    String signature = exchange.getIn().getHeader(PAYSTACK_VERIFICATION.getName(), String.class);
                    if (!webhookInboxService.verifyPaystackSignature(rawBody, signature)) {
                        throw new SecurityException("Invalid Paystack webhook signature");
                    }
                })
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(exchange -> {
                    Map<String, Object> event = exchange.getIn().getBody(Map.class);
                    Map<String, Object> data = (Map<String, Object>) event.get("data");

                    exchange.getIn().setBody(data);
                    exchange.getIn().setHeader("webhookEventId", String.valueOf(data.get("id")));
                    exchange.getIn().setHeader("payment_reference", String.valueOf(data.get("reference")));
                    exchange.getIn().setHeader("gatewayStatus", String.valueOf(data.get("status"))); // success | failed | abandoned
                })

                // ── Idempotency: reuse the inbox pattern, same as your Kafka consumers ──
                .bean(webhookInboxService, "claim")
                .choice()
                .when(simple("${body} == false"))
                .log(LoggingLevel.INFO, "Duplicate Paystack webhook, skipping → reference: ${header.payment_reference}")
                .stop()
                .end()

                // ── Look up the order this payment belongs to ──
                .to("sql:SELECT order_id FROM payments WHERE payment_reference = CAST(:#payment_reference AS UUID)?dataSource=#postgresDataSource&outputType=SelectOne")
                .process(exchange -> exchange.getIn().setHeader("orderId", exchange.getIn().getBody(Map.class).get("order_id")))

                .choice()
                .when(simple("${header.gatewayStatus} == 'success'"))
                .process(exchange -> paymentSuccessService.recordSuccess(
                        exchange.getIn().getHeader("payment_reference", "", String.class),
                        exchange.getIn().getHeader("orderId", "", String.class)))
                .process(exchange -> exchange.getIn().setBody(Map.of(
                        "orderId", exchange.getIn().getHeader("orderId"),
                        "status", "SUCCESS"
                )))
                .marshal().json(JsonLibrary.Jackson)

                //  — see OrderRouteBuilder.
                .to("kafka:PAYMENT_SUCCESS_TOPIC?brokers={{spring.kafka.bootstrap-servers}}")
                .otherwise()
                .process(exchange -> paymentFailureService.recordFailure(
                        exchange.getIn().getHeader("payment_reference", "", String.class),
                        exchange.getIn().getHeader("orderId", "", String.class)))
                .end();

        tryDef
                .doCatch(SecurityException.class)
                .log(LoggingLevel.WARN, "Rejected Paystack webhook: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Webhook processing error: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .end();
    }


}
