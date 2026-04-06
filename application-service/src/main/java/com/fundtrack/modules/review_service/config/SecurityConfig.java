package com.fundtrack.modules.review_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration("reviewSecurityConfig")
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1) // ADD THIS: Gives this chain priority over the default one
    public SecurityFilterChain reviewfilterChain(HttpSecurity http) throws Exception {
        
        // ADD THIS: Tells Spring this chain ONLY cares about /api/v1/reviews/ URLs
        http.securityMatcher("/api/v1/reviews/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/reviews/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            
        return http.build();
    }
}