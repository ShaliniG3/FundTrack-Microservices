package com.cts.fundtrack.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
// FIX: Explicitly tell Feign to scan both the local and common client packages
@EnableFeignClients(basePackages = {
    "com.cts.fundtrack.application",
    "com.cts.fundtrack.common.client"
})
// FIX: Ensure ComponentScan also covers the common package for things like the Aspect itself if needed
@ComponentScan(basePackages = {
    "com.cts.fundtrack.application",
    "com.cts.fundtrack.common"
})
public class FundtrackApplicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundtrackApplicationServiceApplication.class, args);
    }
}