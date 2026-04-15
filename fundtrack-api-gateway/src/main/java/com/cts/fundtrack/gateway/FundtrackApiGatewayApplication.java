package com.cts.fundtrack.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FundtrackApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundtrackApiGatewayApplication.class, args);
    }

    /**
     * Programmatically defines the API Gateway routes.
     * This bypasses application.yml/properties for routing logic.
     */
    @Bean
    public RouteLocator fundtrackRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            // 1. Identity Service Route
            .route("identity-service", r -> r.path("/api/v1/auth/**")
                .uri("lb://FUNDTRACK-IDENTITY-SERVICE"))
            
            // 2. Program Service Route
            .route("program-service", r -> r.path("/api/programs/**")
                .uri("lb://FUNDTRACK-PROGRAM-SERVICE"))
            
            // 3. Lifecycle Service Route
            .route("lifecycle-service", r -> r.path("/api/lifecycle/**")
                .uri("lb://FUNDTRACK-LIFECYCLE-SERVICE"))
            .build();
    }
}