package com.mustycodified.musty_create_order.processors;

import com.mustycodified.musty_create_order.commonlib.models.OrderResponse;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.math.BigDecimal;

public class OrderResponseDTOProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String orderId = exchange.getIn().getHeader("orderId", String.class);
        String userId = exchange.getIn().getHeader("userId", String.class);
        BigDecimal totalPrice = exchange.getIn().getHeader("totalPrice", BigDecimal.class);
        Integer quantity = exchange.getIn().getHeader("quantity", Integer.class);

        OrderResponse dto = new OrderResponse();
        dto.setId(orderId);
        dto.setUid(userId);
        dto.setQuantity(quantity);
        dto.setTotalPrice(totalPrice);
        dto.setStatus("PENDING");
        exchange.getIn().setBody(dto);
    }
}
