package com.cts.fundtrack.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.cts.fundtrack"}) // Scans both .identity and .common
public class FundtrackIdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FundtrackIdentityServiceApplication.class, args);
    }
}