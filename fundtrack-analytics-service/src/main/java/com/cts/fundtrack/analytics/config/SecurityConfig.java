package com.cts.fundtrack.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cts.fundtrack.analytics.security.GatewayHeaderFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration for the Analytics Microservice.
 *
 * <p>This service operates in a stateless, gateway-first security model:
 * JWT validation is performed upstream by the API Gateway, which then stamps
 * authenticated requests with {@code X-User-Id} and {@code X-User-Roles} headers.
 * The {@link GatewayHeaderFilter} reads these headers and populates the
 * {@link org.springframework.security.core.context.SecurityContext} so that
 * method-level authorization (via {@code @PreAuthorize}) functions correctly.</p>
 *
 * <p>CSRF protection is disabled because this is a stateless REST API with no
 * browser-based session management.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize on Analytics endpoints
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderFilter gatewayHeaderFilter;

    /**
     * Configures the HTTP security filter chain for the Analytics Service.
     *
     * <ul>
     *   <li>Disables CSRF (stateless API, no session cookies).</li>
     *   <li>Enforces stateless session management.</li>
     *   <li>Permits unauthenticated access to Actuator and Swagger/OpenAPI endpoints.</li>
     *   <li>Requires authentication for all other requests.</li>
     *   <li>Registers {@link GatewayHeaderFilter} before the standard username/password
     *       filter to inject the security context from gateway-propagated headers.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration cannot be applied
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Stateless APIs don't need CSRF
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            // Inject our custom header filter before the standard auth filter
            .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}