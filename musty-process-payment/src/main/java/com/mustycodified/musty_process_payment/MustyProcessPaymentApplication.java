package com.mustycodified.musty_process_payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.mustycodified.musty_process_payment", "com.mustycodified.commonlib"})
public class MustyProcessPaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MustyProcessPaymentApplication.class, args);
	}

}
