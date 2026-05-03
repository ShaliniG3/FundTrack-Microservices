package com.cts.fundtrack.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class FundtrackDiscoveryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FundtrackDiscoveryServiceApplication.class, args);
	}

}
