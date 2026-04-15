package com.cts.fundtrack.analytics.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig; // 1. IMPORT FROM COMMON
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;

/**
 * Feign client for communicating with the application-service.
 * Linked to the shared FeignConfig for header propagation.
 */
@FeignClient(
    name = "fundtrack-application-service", 
    configuration = FeignConfig.class
)
public interface ApplicationClient {

    @GetMapping("/api/v1/applications/program/{programId}")
    List<ApplicationResponseDTO> getApplicationsByProgram(@PathVariable("programId") UUID programId);

    @GetMapping("/api/v1/programs/{programId}")
    ProgramResponseDTO getProgramDetails(@PathVariable("programId") UUID programId);
}