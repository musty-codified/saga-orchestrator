package com.mustycodified.musty_create_order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(
        exclude = {KafkaAutoConfiguration.class},
        scanBasePackages = {"com.mustycodified.musty_create_order", "com.mustycodified.commonlib"}
)
public class MustyCreateOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(MustyCreateOrderApplication.class, args);

	}
}