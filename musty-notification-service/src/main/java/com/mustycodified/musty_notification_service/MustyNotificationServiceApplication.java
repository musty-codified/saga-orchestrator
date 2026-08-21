package com.mustycodified.musty_notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.mustycodified.musty_notification_service", "com.mustycodified.commonlib"})
public class MustyNotificationServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MustyNotificationServiceApplication.class, args);
	}

}
