package com.cts.fundtrack.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Main Entry Point for the Analytics Microservice.
 * * scanBasePackages: Essential to find common configs and aspects in com.cts.fundtrack.common.
 * basePackages (Feign): Essential to find AuditClient in the common module.
 */
@SpringBootApplication(scanBasePackages = "com.cts.fundtrack")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cts.fundtrack") // 👈 Changed to scan the whole tree
@EnableAspectJAutoProxy
public class FundtrackAnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundtrackAnalyticsServiceApplication.class, args);
    }

}