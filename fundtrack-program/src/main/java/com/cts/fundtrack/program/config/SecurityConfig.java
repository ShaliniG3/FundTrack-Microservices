package com.cts.fundtrack.program.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cts.fundtrack.program.security.GatewayHeaderFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // CRITICAL: This enables @PreAuthorize in your Review/Decision controllers
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF for our stateless microservice
            .csrf(csrf -> csrf.disable())
            
            // 2. Set session to STATELESS (we trust the Gateway's token validation)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 3. Define the rules of the road
            .authorizeHttpRequests(auth -> auth
                // Allow internal health checks and documentation without auth
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // All other endpoints (Applications, Reviews, Decisions) require valid headers
                .anyRequest().authenticated()
            )
            
            // 4. Inject the GatewayHeaderFilter to convert HTTP headers into a Security Context
            .addFilterBefore(new GatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}