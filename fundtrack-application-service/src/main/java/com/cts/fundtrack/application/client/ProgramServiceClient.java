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
 * Feign client for communicating with the {@code fundtrack-program-service} microservice.
 *
 * <p>Provides typed HTTP methods for retrieving eligibility rules and program
 * requirements that are defined and owned by the Program Service. The
 * {@link FeignConfig} configuration propagates the gateway security headers
 * (e.g., {@code X-User-Id}, {@code X-User-Roles}) on every outbound request,
 * preventing 403 responses from the downstream service.</p>
 *
 * <p>If the Program Service is unavailable or returns repeated errors, the
 * circuit breaker automatically delegates calls to {@link ProgramServiceFallback},
 * which returns safe empty/null defaults so the Application Service can degrade
 * gracefully.</p>
 */
@FeignClient(name = "fundtrack-program-service", configuration = FeignConfig.class, fallback = ProgramServiceFallback.class)
public interface ProgramServiceClient {

    /**
     * Retrieves the eligibility rules associated with a grant program.
     *
     * <p>Each rule contains a SpEL expression and a human-readable description
     * used by the automated validation engine to assess whether an applicant
     * meets the program's criteria.</p>
     *
     * @param programId the UUID of the grant program whose rules are requested
     * @return a list of {@link EligibilityRuleDTO} objects; may be empty if no
     *         rules are defined or if the fallback is active
     */
    @GetMapping("/api/v1/programs/{programId}/rules")
    List<EligibilityRuleDTO> getRulesByProgramId(@PathVariable("programId") UUID programId);

    /**
     * Retrieves the consolidated requirements checklist for a grant program.
     *
     * <p>The returned DTO combines required document types and eligibility rule
     * summaries so that applicants and reviewers can see the full set of
     * submission prerequisites in a single call.</p>
     *
     * @param programId the UUID of the grant program whose requirements are requested
     * @return a {@link ProgramRequirementsDTO} with documents and rules, or
     *         {@code null} if the fallback is active
     */
    @GetMapping("/api/v1/programs/{programId}/requirements")
    ProgramRequirementsDTO getRequirements(@PathVariable("programId") UUID programId);
}