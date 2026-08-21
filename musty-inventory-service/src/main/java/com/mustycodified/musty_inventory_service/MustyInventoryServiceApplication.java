package com.mustycodified.musty_inventory_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(
        exclude = {KafkaAutoConfiguration.class},
        scanBasePackages = {"com.mustycodified.musty_inventory_service", "com.mustycodified.commonlib"}
)
public class MustyInventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MustyInventoryServiceApplication.class, args);
    }
}