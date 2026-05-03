package com.cts.fundtrack.disbursement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cts.fundtrack.disbursement.security.GatewayHeaderFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration for the FundTrack Disbursement Service.
 * <p>
 * Configures a stateless, JWT-free security model where authentication context is
 * established by trusting headers injected by the upstream API Gateway
 * ({@code X-User-Id} and {@code X-User-Roles}), processed by {@link GatewayHeaderFilter}.
 * </p>
 * <p>
 * Key security decisions:
 * <ul>
 *   <li>CSRF is disabled — this service is stateless and uses no browser sessions.</li>
 *   <li>Session creation is set to {@code STATELESS} — no HTTP session is created or used.</li>
 *   <li>Actuator, OpenAPI docs, and Swagger UI endpoints are publicly accessible.</li>
 *   <li>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} annotations on
 *       controller methods for fine-grained role-based access control.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderFilter gatewayHeaderFilter;

    /**
     * Defines the primary {@link SecurityFilterChain} for the disbursement service.
     * <p>
     * Registers the {@link GatewayHeaderFilter} before Spring Security's default
     * {@link UsernamePasswordAuthenticationFilter} so that gateway-propagated identity
     * headers are translated into a {@link org.springframework.security.core.Authentication}
     * object before any authorization decisions are made.
     * </p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if any security configuration step fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v1/files/**").permitAll()  // ✅ ADD THIS
                        .anyRequest().authenticated()
                )
            .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}