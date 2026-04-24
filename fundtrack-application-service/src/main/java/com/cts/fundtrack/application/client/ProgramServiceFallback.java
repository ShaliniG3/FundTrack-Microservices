package com.cts.fundtrack.application.client;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;
import com.cts.fundtrack.common.exceptions.ServiceUnavailableException;

/**
 * Circuit breaker fallback for {@link ProgramServiceClient}.
 *
 * <p>Activated when the Program Service is unreachable or returns repeated errors.
 * Rather than returning silent defaults (such as {@code null} or empty collections)
 * that could corrupt downstream business logic, this fallback throws a
 * {@link ServiceUnavailableException} to signal explicitly that the dependency
 * is temporarily unavailable.</p>
 *
 * <p>Callers are expected to catch {@link ServiceUnavailableException} and respond
 * appropriately — either by deferring the operation, returning a {@code 503} to the
 * client, or applying a safe degradation strategy without mutating application state.</p>
 */
@Component
public class ProgramServiceFallback implements ProgramServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProgramServiceFallback.class);

    /**
     * Throws a {@link ServiceUnavailableException} when the Program Service is unavailable.
     *
     * <p>Previously this method returned an empty list, which caused the validation engine
     * to skip all eligibility checks silently — leaving applications in an inconsistent
     * state. Throwing instead ensures that callers explicitly handle the outage rather
     * than proceeding with zero rules as if validation had passed.</p>
     *
     * @param programId the UUID of the program whose eligibility rules were requested
     * @throws ServiceUnavailableException always, to signal that the Program Service
     *                                     is currently unreachable
     */
    @Override
    public List<EligibilityRuleDTO> getRulesByProgramId(UUID programId) {
        log.warn("[CircuitBreaker] Program Service unavailable — cannot fetch rules for programId={}", programId);
        throw new ServiceUnavailableException("Program Service is currently unavailable. Please try again later.");
    }

    /**
     * Throws a {@link ServiceUnavailableException} when the Program Service is unavailable.
     *
     * <p>Previously this method returned {@code null}, which propagated silently through
     * the application layer and caused callers such as
     * {@code ApplicationServiceImpl#getRequirementsByApplication} to either throw a
     * {@link NullPointerException} or surface misleading error states to the client.
     * Throwing instead gives callers a typed, catchable signal to handle gracefully.</p>
     *
     * @param programId the UUID of the program whose requirements were requested
     * @throws ServiceUnavailableException always, to signal that the Program Service
     *                                     is currently unreachable
     */
    @Override
    public ProgramRequirementsDTO getRequirements(UUID programId) {
        log.warn("[CircuitBreaker] Program Service unavailable — cannot fetch requirements for programId={}", programId);
        throw new ServiceUnavailableException("Program Service is currently unavailable. Please try again later.");
    }
}