package com.example.Rastreopaquetes;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RastreopaquetesApplication {

	private static ConfigurableApplicationContext springContext;

	public static void main(String[] args) {
		System.getProperties().put("server.port", "8081");
		SpringApplication.run(RastreopaquetesApplication.class, args);
		Application.launch(RastreoApp.class, args);
	}
	public static ConfigurableApplicationContext getSpringContext() {
		return springContext;
	}

}
