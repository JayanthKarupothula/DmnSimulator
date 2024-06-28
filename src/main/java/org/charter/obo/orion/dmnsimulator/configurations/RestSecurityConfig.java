package org.charter.obo.orion.dmnsimulator.configurations;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Order(SecurityProperties.BASIC_AUTH_ORDER - 20)
public class RestSecurityConfig extends WebSecurityConfigurerAdapter {

  /**
   * This method is used to configure the HTTP security settings for the "/evaluateDecision" endpoint.
   * It sets the endpoint to use Cross-Origin Resource Sharing (CORS) and disables Cross-Site Request Forgery (CSRF) protection.
   *
   * @param http The HttpSecurity object to be modified.
   * @throws Exception If an error occurs while configuring the HTTP security.
   */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/evaluateDecision").cors().and().csrf().disable();
    }

}
