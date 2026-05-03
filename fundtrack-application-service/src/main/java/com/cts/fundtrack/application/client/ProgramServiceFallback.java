package com.cts.fundtrack.application.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;

/**
 * Circuit breaker fallback for {@link ProgramServiceClient}.
 *
 * <p>Activated when the Program Service is unreachable or returns repeated errors.
 * Returns safe defaults that allow the application service to degrade gracefully
 * rather than failing with a cascading error.</p>
 */
@Component
public class ProgramServiceFallback implements ProgramServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProgramServiceFallback.class);

    /**
     * Returns an empty rules list when the Program Service is unavailable.
     * An empty list means no eligibility rules will be evaluated — the application
     * proceeds without validation rather than being blocked.
     *
     * @param programId the program whose rules were requested
     * @return an empty list
     */
    @Override
    public List<EligibilityRuleDTO> getRulesByProgramId(UUID programId) {
        log.warn("[CircuitBreaker] Program Service unavailable — returning empty rules for programId={}", programId);
        return Collections.emptyList();
    }

    /**
     * Returns {@code null} when the Program Service is unavailable.
     * Callers must null-check this return value before use.
     *
     * @param programId the program whose requirements were requested
     * @return {@code null}
     */
    @Override
    public ProgramRequirementsDTO getRequirements(UUID programId) {
        log.warn("[CircuitBreaker] Program Service unavailable — returning null requirements for programId={}", programId);
        return null;
    }
}
