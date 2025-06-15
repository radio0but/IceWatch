package com.radioip.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RadioBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RadioBackendApplication.class, args);
	}

}
