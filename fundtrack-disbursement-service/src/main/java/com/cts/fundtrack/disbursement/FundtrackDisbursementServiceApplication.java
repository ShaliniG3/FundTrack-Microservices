package com.cts.fundtrack.disbursement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main entry point for the FundTrack Disbursement Microservice.
 * <p>
 * This service is responsible for the core financial execution layer of the FundTrack
 * grant funding platform. It manages the full post-award lifecycle, including:
 * <ul>
 *   <li>Budget splitting and installment schedule generation for approved programs</li>
 *   <li>Actual payment transaction recording and receipt generation</li>
 *   <li>Grant progress report submission and storage</li>
 *   <li>Compliance audit workflows and officer dashboards</li>
 * </ul>
 * The service integrates with the Application Service, Program Service, and Notification
 * Service via Feign clients, and registers with the Eureka discovery server for
 * service-to-service communication.
 * </p>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.cloud.client.discovery.EnableDiscoveryClient
 * @see org.springframework.cloud.openfeign.EnableFeignClients
 */
@SpringBootApplication(scanBasePackages = "com.cts.fundtrack") // Scans local AND common modules
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cts.fundtrack")      // Finds both local and shared Feign clients
public class FundtrackDisbursementServiceApplication {

    /**
     * Application bootstrap method. Launches the Spring Boot context for the
     * Disbursement Microservice.
     *
     * @param args command-line arguments passed to the JVM at startup
     */
    public static void main(String[] args) {
        SpringApplication.run(FundtrackDisbursementServiceApplication.class, args);
    }

}