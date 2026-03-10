package com.mustycodified.musty_create_order.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.math.BigDecimal;
import java.util.Map;

public class KafkaMessageBuilderProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String orderId = exchange.getIn().getHeader("orderId", String.class);
        String userId = exchange.getIn().getHeader("userId", String.class);

        String  rawProductId = exchange.getIn().getHeader("variantProductId", String.class);
        Integer variantProductId = Integer.parseInt(rawProductId);
        exchange.getIn().setHeader("variantProductId", variantProductId);

        BigDecimal totalPrice = exchange.getIn().getHeader("totalPrice", BigDecimal.class);
        Integer quantity = exchange.getIn().getHeader("quantity", Integer.class);
        //Build kafka message
        Map<String, Object> kafkaMessage = Map.of(
                "orderId", orderId,
                "userId", userId,
                "variantProductId", variantProductId,
                "totalPrice", totalPrice,
                "quantity", quantity
        );
        exchange.getIn().setBody(kafkaMessage);
    }
}
