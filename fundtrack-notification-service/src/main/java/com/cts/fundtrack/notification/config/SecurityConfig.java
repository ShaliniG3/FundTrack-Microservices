package com.cts.fundtrack.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cts.fundtrack.notification.security.GatewayHeaderFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration for the Notification Service.
 *
 * <p>This service sits behind the API Gateway, which validates JWT tokens and
 * injects identity headers ({@code X-User-Role}, {@code X-User-Email}) into
 * forwarded requests. The Notification Service therefore does not perform its
 * own JWT verification; instead, it trusts the gateway-injected headers and
 * delegates authentication to the {@link GatewayHeaderFilter}.</p>
 *
 * <p>Security policy summary:</p>
 * <ul>
 *   <li>CSRF protection is disabled — not required for stateless REST APIs.</li>
 *   <li>Sessions are never created ({@link SessionCreationPolicy#STATELESS});
 *       all identity state is carried per-request in gateway headers.</li>
 *   <li>Swagger UI and OpenAPI spec endpoints are publicly accessible.</li>
 *   <li>All other endpoints require an authenticated principal established
 *       by the {@link GatewayHeaderFilter}.</li>
 *   <li>Method-level security ({@code @PreAuthorize} / {@code @PostAuthorize})
 *       is enabled via {@code @EnableMethodSecurity}.</li>
 * </ul>
 *
 * @see GatewayHeaderFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderFilter gatewayHeaderFilter;

    /**
     * Configures the main {@link SecurityFilterChain} for the Notification Service.
     *
     * <p>The {@link GatewayHeaderFilter} is inserted before
     * {@link UsernamePasswordAuthenticationFilter} so that the gateway-provided
     * identity headers are translated into a Spring Security
     * {@link org.springframework.security.core.Authentication} object before any
     * access-control decisions are made.</p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if any security configuration step fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            // Add your filter before the standard Spring Security filters
            .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}