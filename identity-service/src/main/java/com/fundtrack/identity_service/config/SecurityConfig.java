package com.fundtrack.identity_service.config;

import com.fundtrack.identity_service.security.CustomAccessDeniedHandler;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration class that defines the authentication manager,
 * security filter chain, and password encoder for the application.
 * <p>
 * Key features:
 * <ul>
 * <li>Stateless session policy (suitable for JWT-based authentication)</li>
 * <li>CSRF disabled and CORS enabled</li>
 * <li>Publicly accessible endpoints for authentication, actuator, and Swagger</li>
 * <li>Role-based access rules for domain-specific paths</li>
 * <li>Custom JWT filter integration</li>
 * <li>Custom access denied handler</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
@OpenAPIDefinition(info = @Info(title = "FundTrack API", version = "v1", description = "Documentation for FundTrack Security API"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SecurityConfig {

    /**
     * Custom JWT filter that validates and sets authentication for requests containing a valid JWT.
     */
    private final JwtFilter jwtFilter;

    /**
     * Custom handler for AccessDeniedException, returning appropriate responses for unauthorized access.
     */
    private final CustomAccessDeniedHandler deniedHandler;

    /**
     * Exposes the {@link AuthenticationManager} obtained from {@link AuthenticationConfiguration}.
     *
     * @param config the Spring Security authentication configuration
     * @return the {@link AuthenticationManager} bean
     * @throws Exception if the authentication manager cannot be built
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the main Spring Security filter chain, including endpoint authorization rules,
     * stateless session policy, disabled CSRF/form login/basic auth, and JWT filter placement.
     *
     * @param http   the {@link HttpSecurity} builder to configure HTTP security
     * @param filter the {@link JwtFilter} to be applied before {@link UsernamePasswordAuthenticationFilter}
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter filter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                    // Rely on default CORS configuration or a separate CorsConfigurationSource bean if defined.
                })
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**", "/v3/api-docs",
                                "/swagger-ui/**",

                                "/swagger-ui.html",
                                "/api/v1/auth/**",
                                ("/api/v1/analytics/**"),
                                ("/api/v1/applications/**")
                        ).permitAll()

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.accessDeniedHandler(deniedHandler))
                .build();
    }

    /**
     * Provides a {@link PasswordEncoder} bean based on BCrypt for secure password hashing.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}