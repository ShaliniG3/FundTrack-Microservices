package com.cts.fundtrack.application.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig;
import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;

/**
 * Communicates with the Program Microservice.
 * configuration = FeignConfig.class is what fixes the 403 error by passing headers.
 */
@FeignClient(name = "fundtrack-program-service", configuration = FeignConfig.class)
public interface ProgramServiceClient {

    /**
     * Fetches the SpEL expressions and descriptions for validation logic.
     */
    @GetMapping("/api/v1/programs/{programId}/rules")
    List<EligibilityRuleDTO> getRulesByProgramId(@PathVariable("programId") UUID programId);

    /**
     * Fetches the consolidated checklist of docs and rules for the application process.
     */
    @GetMapping("/api/v1/programs/{programId}/requirements")
    ProgramRequirementsDTO getRequirements(@PathVariable("programId") UUID programId);
}