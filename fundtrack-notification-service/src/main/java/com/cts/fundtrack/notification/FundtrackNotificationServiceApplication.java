package com.cts.fundtrack.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the Notification Service.
 * * scanBasePackages: Tells Spring to look in the 'common' module for Aspects, Filters, and Configs.
 * EnableFeignClients: Specifically searches for @FeignClient interfaces in the shared packages.
 */
@SpringBootApplication(scanBasePackages = "com.cts.fundtrack")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cts.fundtrack")
public class FundtrackNotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundtrackNotificationServiceApplication.class, args);
    }

}