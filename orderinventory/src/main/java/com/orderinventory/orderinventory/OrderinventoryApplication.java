package com.orderinventory.orderinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.cache.annotation.EnableCaching
public class OrderinventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderinventoryApplication.class, args);
	}

}
