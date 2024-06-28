package org.charter.obo.orion.dmnsimulator.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * This class is used to configure Cross-Origin Resource Sharing (CORS) for the "/evaluateDecision" endpoint.
 * It allows credentials, all origins, all headers, and all methods for CORS requests to this endpoint.
 */
@Configuration
public class RestConfiguration {

  /**
   * This method is used to create a CORS configuration source.
   * It creates a CORS configuration that allows credentials, all origins, all headers, and all methods.
   * It then creates a URL-based CORS configuration source, registers the CORS configuration for the "/evaluateDecision" endpoint, and returns the configuration source.
   *
   * @return The CORS configuration source.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    // Create a new CORS configuration
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow credentials
    configuration.setAllowCredentials(true);

    // Allow all origins
    configuration.addAllowedOrigin("*");

    // Allow all headers
    configuration.addAllowedHeader("*");

    // Allow all methods
    configuration.addAllowedMethod("*");

    // Create a new URL-based CORS configuration source
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // Register the CORS configuration for the "/evaluateDecision" endpoint
    source.registerCorsConfiguration("/evaluateDecision", configuration);

    // Return the CORS configuration source
    return source;
  }
}
