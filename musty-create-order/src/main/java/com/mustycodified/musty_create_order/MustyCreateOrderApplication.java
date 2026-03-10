package com.mustycodified.musty_create_order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class}) // This kills Producer-2@Slf4j
public class MustyCreateOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(MustyCreateOrderApplication.class, args);

	}
}