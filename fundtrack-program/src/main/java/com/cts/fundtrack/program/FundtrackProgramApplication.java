package com.cts.fundtrack.program;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the FundTrack Program Microservice.
 *
 * <p>This microservice is responsible for the full lifecycle management of grant funding
 * programs within the FundTrack system. It handles creation, retrieval, status transitions,
 * archival, and expiry of programs, as well as exposing internal Feign-compatible endpoints
 * consumed by other microservices (e.g., the Application Service).</p>
 *
 * <p>Key capabilities enabled by this bootstrap class:</p>
 * <ul>
 *   <li>{@code @SpringBootApplication} — auto-configures and component-scans the entire
 *       {@code com.cts.fundtrack} base package, which includes shared aspects and filters
 *       from the common module.</li>
 *   <li>{@code @EnableFeignClients} — activates declarative REST clients (e.g.,
 *       {@code NotificationClient}, {@code AuditClient}) defined in the common module.</li>
 *   <li>{@code @EnableScheduling} — activates the {@link com.cts.fundtrack.program.service.ProgramScheduler}
 *       for time-driven tasks such as auto-expiring programs past their end date.</li>
 *   <li>{@code @EnableDiscoveryClient} — registers this service with the Eureka Service
 *       Registry so it can be discovered and load-balanced by the API Gateway and peer services.</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.cts.fundtrack") // 👈 Scans common for Aspects/Filters
@EnableFeignClients(basePackages = "com.cts.fundtrack")      // 👈 Scans common for Feign Clients
@EnableScheduling
@EnableDiscoveryClient
public class FundtrackProgramApplication {

    /**
     * Application entry point. Bootstraps the Spring Boot context for the Program Microservice.
     *
     * @param args command-line arguments passed at startup (forwarded to Spring Boot).
     */
    public static void main(String[] args) {
        SpringApplication.run(FundtrackProgramApplication.class, args);
    }

}
