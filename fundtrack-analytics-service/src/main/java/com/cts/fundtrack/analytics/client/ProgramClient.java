package com.cts.fundtrack.analytics.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;

@FeignClient(
    name = "fundtrack-program-service",
    configuration = FeignConfig.class,
    fallback = ProgramClientFallback.class
)
public interface ProgramClient {

    @GetMapping("/api/v1/programs/{programId}")
    ProgramResponseDTO getProgramDetails(@PathVariable("programId") UUID programId);
}
