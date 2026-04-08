package com.cts.fundtrack.dgcs.client.programclient;


import com.cts.fundtrack.dgcs.client.dto.ProgramMetadataDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "program-service", fallback = ProgramFallback.class)
public interface ProgramClient {
    @GetMapping("/api/internal/programs/{id}")
    ProgramMetadataDTO getProgramById(@PathVariable("id") UUID id);
}

