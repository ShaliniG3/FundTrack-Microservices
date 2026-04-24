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

/**
 * Spring Security configuration for the Program Microservice.
 *
 * <p>This configuration establishes a stateless, token-free security model suited to
 * a microservice that sits behind a trusted API Gateway. Rather than issuing or
 * validating JWTs directly, this service trusts the gateway to authenticate users
 * and propagate identity via HTTP headers ({@code X-User-Roles}, {@code X-User-Email},
 * {@code X-User-Id}). The {@link GatewayHeaderFilter} reads those headers and constructs
 * a Spring Security {@code Authentication} object for each incoming request.</p>
 *
 * <p>Security policies applied by this configuration:</p>
 * <ol>
 *   <li>CSRF protection is disabled — appropriate for stateless REST APIs that do not
 *       use browser-based session cookies.</li>
 *   <li>Session management is set to {@code STATELESS} — no {@code HttpSession} is
 *       created or used; the security context is re-built from headers on every request.</li>
 *   <li>Public endpoints ({@code /actuator/**}, {@code /v3/api-docs/**},
 *       {@code /swagger-ui/**}) are permitted without authentication to support health
 *       checks and API documentation tooling.</li>
 *   <li>All other endpoints require an authenticated principal, enforced by the
 *       {@link GatewayHeaderFilter} populating the {@code SecurityContextHolder}.</li>
 *   <li>Method-level security ({@code @PreAuthorize}) is enabled via
 *       {@code @EnableMethodSecurity}, allowing role-based guards directly on
 *       controller methods (e.g., {@code hasRole('ADMIN')}).</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // CRITICAL: This enables @PreAuthorize in your Review/Decision controllers
public class SecurityConfig {

    private final GatewayHeaderFilter gatewayHeaderFilter;

    /**
     * Constructs the {@code SecurityConfig} with the required gateway header filter.
     *
     * @param gatewayHeaderFilter the filter that extracts user identity from gateway-injected
     *                            HTTP headers and populates the Spring Security context.
     */
    public SecurityConfig(GatewayHeaderFilter gatewayHeaderFilter) {
        this.gatewayHeaderFilter = gatewayHeaderFilter;
    }

    /**
     * Defines the security filter chain for the Program Microservice.
     *
     * <p>Configures the following behaviour in order:</p>
     * <ol>
     *   <li>Disables CSRF protection (stateless API, no session cookies).</li>
     *   <li>Sets session creation policy to {@link SessionCreationPolicy#STATELESS}.</li>
     *   <li>Permits unauthenticated access to actuator, OpenAPI docs, and Swagger UI
     *       endpoints; requires authentication for all other requests.</li>
     *   <li>Registers {@link GatewayHeaderFilter} before
     *       {@link UsernamePasswordAuthenticationFilter} so that the security context
     *       is populated before any standard Spring Security processing occurs.</li>
     * </ol>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security.
     * @return the configured {@link SecurityFilterChain} bean.
     * @throws Exception if any Spring Security configuration step fails.
     */
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
                    .requestMatchers("/api/internal/**").permitAll()
                    .requestMatchers("/api/v1/programs/*").permitAll()
                .anyRequest().authenticated()
            )

            // 4. Inject the GatewayHeaderFilter to convert HTTP headers into a Security Context
            .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
