package com.cts.fundtrack.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cts.fundtrack.application.security.GatewayHeaderFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration for the Application Service.
 *
 * <p>This service operates as a stateless microservice behind an API Gateway.
 * Authentication is not performed here; instead, the gateway validates the JWT
 * and forwards trusted headers ({@code X-User-Id}, {@code X-User-Roles},
 * {@code X-User-Email}) that are consumed by {@link GatewayHeaderFilter} to
 * reconstruct the security context for every request.</p>
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>CSRF is disabled — microservice APIs are stateless and do not use
 *       browser-based session cookies that CSRF exploits.</li>
 *   <li>Sessions are {@code STATELESS} — no server-side session storage is
 *       used; each request is independently authenticated via headers.</li>
 *   <li>Method-level security ({@code @PreAuthorize}) is enabled, allowing
 *       role-based access control to be declared directly on controller
 *       methods.</li>
 *   <li>Actuator and OpenAPI documentation endpoints are publicly accessible
 *       for health checks and API discoverability.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Allows @PreAuthorize("hasRole('...')") to work in your Controller
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderFilter gatewayHeaderFilter;

    /**
     * Defines the {@link SecurityFilterChain} that governs all HTTP security
     * rules for the Application Service.
     *
     * <p>The filter chain:
     * <ol>
     *   <li>Disables CSRF protection (not needed for stateless REST APIs).</li>
     *   <li>Enforces a {@code STATELESS} session policy.</li>
     *   <li>Permits unauthenticated access to actuator and Swagger/OpenAPI
     *       endpoints; all other requests require authentication.</li>
     *   <li>Registers {@link GatewayHeaderFilter} ahead of the standard
     *       username/password filter so that gateway-injected headers are
     *       translated into a Spring Security {@code Authentication} object
     *       before any authorization checks run.</li>
     * </ol>
     * </p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if any Spring Security configuration step fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF (Crucial for microservices to stop the "Invalid CSRF Token" 403)
            .csrf(csrf -> csrf.disable())
            
            // 2. Set session to STATELESS (we don't store user info in RAM; we use headers)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 3. Define the rules for the application routes
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/applications/programs/*/winners").permitAll()
                    .requestMatchers("/api/v1/applications/pending-reviews/**").permitAll()
                    .requestMatchers("/api/internal/**").permitAll()
                    .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                    .requestMatchers("/api/v1/applications/programs/*/approved").permitAll()
                    .requestMatchers("/api/v1/applications/programs/*/accepted").permitAll()
                    .requestMatchers("/api/v1/application-files/**").permitAll()
                    .anyRequest().authenticated()
            )
            
            // 4. Inject the filter that reads headers from the API Gateway
            .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}