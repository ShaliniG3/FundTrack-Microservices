package com.cts.fundtrack.analytics.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;

@FeignClient(
        name = "fundtrack-application-service",
        configuration = FeignConfig.class,
        fallback = ApplicationClientFallback.class
)
public interface ApplicationClient {

    @GetMapping("/api/v1/applications/programs/{programId}/accepted")
    List<ApplicationResponseDTO> getApplicationsByProgram(@PathVariable("programId") UUID programId);
}