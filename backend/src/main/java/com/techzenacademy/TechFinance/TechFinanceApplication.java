package com.techzenacademy.TechFinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.techzenacademy.TechFinance",
    "com.techzenacademy.TechFinance.controller",
    "com.techzenacademy.TechFinance.service",
    "com.techzenacademy.TechFinance.service.impl",
    "com.techzenacademy.TechFinance.repository"
})
public class TechFinanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechFinanceApplication.class, args);
	}

}
