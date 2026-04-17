package com.cts.fundtrack.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Main entry point for the FundTrack Analytics Microservice.
 *
 * <p>This Spring Boot application exposes endpoints for program-level analytics,
 * financial summaries, and daily application trend analysis. It aggregates data
 * from the Application Service and Finance Service via Feign clients.</p>
 *
 * <p>Key bootstrap configurations:</p>
 * <ul>
 *   <li>{@code scanBasePackages = "com.cts.fundtrack"} - Ensures that shared beans
 *       defined in {@code fundtrack-common} (e.g., {@code FeignConfig}, {@code AuditClient},
 *       aspects) are discovered and registered in the application context.</li>
 *   <li>{@code @EnableFeignClients(basePackages = "com.cts.fundtrack")} - Scans the entire
 *       FundTrack package tree so Feign clients in the common module (e.g., {@code AuditClient})
 *       are picked up alongside service-local clients.</li>
 *   <li>{@code @EnableAspectJAutoProxy} - Activates Spring AOP proxy support, enabling
 *       the {@code AuditAspect} to intercept annotated service methods.</li>
 *   <li>{@code @EnableDiscoveryClient} - Registers this service with the Eureka discovery
 *       server so that other microservices can locate it by name.</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.cts.fundtrack")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cts.fundtrack") // 👈 Changed to scan the whole tree
@EnableAspectJAutoProxy
public class FundtrackAnalyticsServiceApplication {

    /**
     * Application entry point. Bootstraps the Spring context and starts the embedded
     * web server.
     *
     * @param args command-line arguments passed at startup (forwarded to Spring Boot)
     */
    public static void main(String[] args) {
        SpringApplication.run(FundtrackAnalyticsServiceApplication.class, args);
    }

}