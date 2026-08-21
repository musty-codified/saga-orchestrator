package com.mustycodified.musty_create_order.processors;

import com.mustycodified.musty_create_order.commonlib.models.RequestPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderMessageBuilderProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String rawUserId = exchange.getIn().getHeader("userId", String.class);
        Integer variantProductId = exchange.getIn().getHeader("variantProductId", Integer.class);
        RequestPayload orderRequest = exchange.getIn().getBody(RequestPayload.class);

        if (rawUserId == null || variantProductId == null) {
            throw new IllegalArgumentException("Missing required path parameters");
        }

        UUID orderId = UUID.randomUUID();
        int userId = Integer.parseInt(rawUserId);

        exchange.getIn().setHeader("userId", userId);
        exchange.getIn().setHeader("variantProductId", variantProductId);
        exchange.getIn().setHeader("orderId", orderId);
        exchange.getIn().setHeader("totalPrice", orderRequest.getTotalPrice());
        exchange.getIn().setHeader("quantity", orderRequest.getQuantity());
    }

}
