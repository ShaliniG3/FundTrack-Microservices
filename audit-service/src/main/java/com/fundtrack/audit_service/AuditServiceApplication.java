package com.fundtrack.audit_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main Entry Point for the Audit Microservice.
 * * Logic Flow:
 * 1. Configuration: Fetches MySQL and Eureka settings from the Config Server.
 * 2. Discovery: Registers itself as "AUDIT-SERVICE" in the Eureka dashboard.
 * 3. Database: Connects to MySQL and updates the 'audit_logs' table schema.
 */
@SpringBootApplication
@EnableDiscoveryClient // Enables registration with Eureka Server
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}