package com.mustycodified.musty_create_order.processors;

import com.mustycodified.musty_create_order.commonlib.models.RequestPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class OrderCreationProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String userId = exchange.getIn().getHeader("userId", String.class);
        Integer variantProductId = exchange.getIn().getHeader("variantProductId", Integer.class);
        RequestPayload orderRequest = exchange.getIn().getBody(RequestPayload.class);
        BigDecimal totalPrice = orderRequest.getTotalPrice();
        Integer quantity = orderRequest.getQuantity();
        if (userId == null || variantProductId == null) {
            throw new IllegalArgumentException("Missing required path parameters");
        }

        exchange.getIn().setHeader("totalPrice", totalPrice);
        exchange.getIn().setHeader("quantity", quantity);
        exchange.getIn().setHeader("userId", userId);
//        exchange.getIn().setHeader("userId", userId);
    }
}
