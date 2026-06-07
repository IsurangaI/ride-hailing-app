package com.ridehailing.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication()
public class AuthServiceApplication {

	static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
