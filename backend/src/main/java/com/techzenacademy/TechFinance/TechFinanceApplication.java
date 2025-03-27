package com.techzenacademy.TechFinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class TechFinanceApplication {

	static {
		try {
			// Load .env file before Spring initializes
			Dotenv dotenv = Dotenv.configure()
				.directory("backend")
				.load();
			
			// Set environment variables for Spring to use
			System.setProperty("GEMINI_API_KEY", dotenv.get("GEMINI_API_KEY"));
			System.setProperty("MYSQL_USER", dotenv.get("MYSQL_USER"));
			System.setProperty("MYSQL_PASSWORD", dotenv.get("MYSQL_PASSWORD"));
			System.setProperty("MYSQL_DATABASE", dotenv.get("MYSQL_DATABASE"));
			System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
			
			System.out.println("Loaded .env from backend directory");
		} catch (Exception e) {
			System.err.println("Warning: Failed to load .env file: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(TechFinanceApplication.class, args);
	}
	
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOrigins("*")
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
						.allowedHeaders("*");
			}
		};
	}
}
