package org.charter.obo.orion.dmnsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity
public class DmnSimulatorApplication {

  // ENTRY POINT
	public static void main(String[] args) {
		SpringApplication.run(DmnSimulatorApplication.class, args);
	}
}
