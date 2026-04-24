package com.cts.fundtrack.identity.config;

import com.cts.fundtrack.identity.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration for the Identity Service.
 *
 * <p>Configures a stateless, JWT-based security model:</p>
 * <ul>
 *   <li>CSRF protection is disabled — not needed for stateless REST APIs.</li>
 *   <li>Public routes ({@code /api/v1/auth/**}, Swagger UI, OpenAPI docs) are
 *       whitelisted; every other endpoint requires authentication.</li>
 *   <li>Sessions are never created ({@link SessionCreationPolicy#STATELESS}); all
 *       state is carried in the JWT token itself.</li>
 *   <li>The {@link JwtFilter} is inserted before
 *       {@link UsernamePasswordAuthenticationFilter} to validate Bearer tokens
 *       on every protected request.</li>
 * </ul>
 *
 * @see JwtFilter
 * @see org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * Configures the main {@link SecurityFilterChain} for the application.
     *
     * <p>Policy summary:</p>
     * <ul>
     *   <li>CSRF disabled — not applicable to stateless token-based APIs.</li>
     *   <li>{@code /api/v1/auth/**}, {@code /v3/api-docs/**}, and
     *       {@code /swagger-ui/**} are publicly accessible without a token.</li>
     *   <li>All other requests must carry a valid JWT Bearer token.</li>
     *   <li>No HTTP session is created or used.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if any security configuration step fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/auth/**", "/api/v1/audit/**", "/api/internal/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides a {@link BCryptPasswordEncoder} bean for hashing and verifying passwords.
     *
     * <p>BCrypt is used throughout the authentication flow to encode passwords
     * before storage and to verify plaintext passwords during login.</p>
     *
     * @return a {@link PasswordEncoder} backed by BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean.
     *
     * <p>Uses {@link AuthenticationConfiguration} rather than manual construction
     * to avoid a circular dependency between {@code SecurityConfig} and
     * {@code CustomUserDetailsService}, which would otherwise cause a
     * {@link StackOverflowError}.</p>
     *
     * @param config the Spring-managed {@link AuthenticationConfiguration}
     * @return the application-wide {@link AuthenticationManager}
     * @throws Exception if the authentication manager cannot be obtained
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
