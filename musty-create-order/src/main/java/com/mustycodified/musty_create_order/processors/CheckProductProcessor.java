package com.mustycodified.musty_create_order.processors;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
@Slf4j
@SuppressWarnings("unchecked")
public class CheckProductProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        List<Map<String, Object>> result = exchange.getIn().getBody(List.class);
        Map<String, Object> prod = result.get(0);
        for (Map.Entry<String, Object> e : prod.entrySet()){
            log.info("Key -> : {}, value -> : {}", e.getKey(), e.getValue());
        }

        if (result.isEmpty()){
             exchange.setProperty("quantity", 0);
             exchange.getIn().setHeader("productExists", false);
             exchange.getIn().setBody(Map.of("message", "Product not found", "status", "DECLINED"));
         }
         else{
             Map<String, Object> product = result.get(0);
             int quantity = ((Number) product.get("quantity")).intValue();
             exchange.setProperty("quantity", quantity);
             exchange.getIn().setHeader("productExists", true);
             exchange.getIn().setBody(product);
         }

    }
}

