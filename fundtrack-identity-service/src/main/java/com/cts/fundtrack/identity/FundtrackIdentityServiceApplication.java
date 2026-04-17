package com.cts.fundtrack.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the FundTrack Identity Service.
 *
 * <p>This microservice is responsible for all authentication and authorization concerns,
 * including user registration, login, logout, JWT token issuance and validation,
 * refresh token lifecycle management, password reset flows, and audit logging of
 * security-relevant events.</p>
 *
 * <p>Key capabilities:</p>
 * <ul>
 *   <li>Issues and validates JSON Web Tokens (JWT) for stateless session management.</li>
 *   <li>Manages refresh tokens to support long-lived user sessions.</li>
 *   <li>Records audit log entries for user actions via AOP aspects.</li>
 *   <li>Registers with Eureka for service discovery by the API Gateway.</li>
 * </ul>
 *
 * <p>The {@link ComponentScan} is broadened to {@code com.cts.fundtrack} so that
 * shared components from the {@code fundtrack-common} module (e.g., aspects, DTOs,
 * exception handlers) are automatically picked up alongside identity-specific beans.</p>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.cloud.client.discovery.EnableDiscoveryClient
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.cts.fundtrack"}) // Scans both .identity and .common
public class FundtrackIdentityServiceApplication {

    /**
     * Application bootstrap method.
     *
     * @param args command-line arguments passed to the JVM at startup
     */
    public static void main(String[] args) {
        SpringApplication.run(FundtrackIdentityServiceApplication.class, args);
    }
}
