package com.cts.fundtrack.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class StorageConfig {
    private String uploadDir = "C:/uploads/proofs/";
}