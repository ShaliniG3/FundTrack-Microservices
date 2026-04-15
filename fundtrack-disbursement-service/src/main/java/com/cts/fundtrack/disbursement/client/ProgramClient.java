package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.dto.ProgramMetadataDTO; // Imported from common
import com.cts.fundtrack.common.config.FeignConfig; // Imported from common

/**
 * Communicates with the Program Service to fetch budget and meta information.
 */
@FeignClient(
    name = "fundtrack-program-service", 
    configuration = FeignConfig.class, // Using the shared harness
    fallback = ProgramFallback.class
)
public interface ProgramClient {

    @GetMapping("/api/internal/programs/{id}")
    ProgramMetadataDTO getProgramById(@PathVariable("id") UUID id);
}