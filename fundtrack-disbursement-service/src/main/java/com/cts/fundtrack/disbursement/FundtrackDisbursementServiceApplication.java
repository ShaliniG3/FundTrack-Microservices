package com.cts.fundtrack.disbursement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main entry point for the FundTrack Disbursement Microservice.
 */
@SpringBootApplication(scanBasePackages = "com.cts.fundtrack") // 👈 Scans local AND common modules
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cts.fundtrack")      // 👈 Finds both local and shared Feign clients
public class FundtrackDisbursementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundtrackDisbursementServiceApplication.class, args);
    }

}