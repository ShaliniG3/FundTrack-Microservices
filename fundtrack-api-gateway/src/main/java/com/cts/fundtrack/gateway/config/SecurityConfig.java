package com.cts.fundtrack.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable) // Disable CSRF for microservices
            .authorizeExchange(exchanges -> exchanges
                // 1. Let all Auth traffic through to Identity Service
                .pathMatchers("/api/v1/auth/**").permitAll() 
                // 2. Allow Swagger/OpenAPI documentation to be visible
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // 3. Everything else requires authentication
                .anyExchange().authenticated()
            );
        return http.build();
    }

    /**
     * This Bean is the "Secret Sauce." 
     * Providing this stops Spring from generating that random console password.
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username("gateway-admin")
            .password("password")
            .roles("ADMIN")
            .build();
        return new MapReactiveUserDetailsService(user);
    }
}