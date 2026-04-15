package com.cts.fundtrack.program;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.cts.fundtrack") // 👈 Scans common for Aspects/Filters
@EnableFeignClients(basePackages = "com.cts.fundtrack")      // 👈 Scans common for Feign Clients
@EnableScheduling
@EnableDiscoveryClient
public class FundtrackProgramApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundtrackProgramApplication.class, args);
    }

}