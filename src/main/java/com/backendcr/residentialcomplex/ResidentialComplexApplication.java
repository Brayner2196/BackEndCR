package com.backendcr.residentialcomplex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResidentialComplexApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResidentialComplexApplication.class, args);
	}
}
