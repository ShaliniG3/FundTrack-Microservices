package com.cts.fundtrack.dgcs.config;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for handling file storage settings.
 * <p>
 * This class maps properties prefixed with {@code file} from the
 * application configuration files (e.g., {@code application.properties}
 * or {@code application.yml}) into Java fields.
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "file")
@Data
@Tag(name = "Storage Configuration", description = "Settings for external file storage and upload directories")
public class StorageConfig {

    /**
     * The directory path where uploaded files (e.g., proofs) will be stored.
     * <p>
     * Defaults to {@code C:/uploads/proofs/} if not overridden in the
     * application configuration.
     * </p>
     */
    @Schema(
            description = "Local or absolute path for file uploads",
            example = "C:/uploads/proofs/",
            defaultValue = "C:/uploads/proofs/"
    )
    private String uploadDir = "C:/uploads/proofs/";
}