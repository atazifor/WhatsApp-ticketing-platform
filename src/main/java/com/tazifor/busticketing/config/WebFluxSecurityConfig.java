package com.tazifor.busticketing.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            //Authorize exchanges:
            .authorizeExchange(exchanges -> exchanges
                // Permit all requests to /webhook and its subpaths
                .pathMatchers("/webhook/**").permitAll()
                // Any other request requires authentication
                .anyExchange().authenticated()
            );
        return http.build();
    }
}



