package com.fundtrack.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fundtrack.modules.application_service.dto.EligibilityRuleDTO;
import com.fundtrack.modules.application_service.dto.ProgramRequirementsDTO;

// @FeignClient(name = "program-service",fallback = ProgramServiceClientFallback.class)
@FeignClient(
    name = "program-service", 
    url = "http://localhost:9091", 
    fallback = ProgramServiceClientFallback.class
)
public interface ProgramServiceClient {

    // Requirement 2.2 & 4.2: Fetch eligibility rules for a specific program
    @GetMapping("/api/v1/programs/{programId}/rules")
    List<EligibilityRuleDTO> getRulesByProgramId(@PathVariable("programId") UUID programId);

    @GetMapping("/api/v1/programs/{programId}/requirements")
    ProgramRequirementsDTO getRequirements(@PathVariable("programId") UUID programId);
}
