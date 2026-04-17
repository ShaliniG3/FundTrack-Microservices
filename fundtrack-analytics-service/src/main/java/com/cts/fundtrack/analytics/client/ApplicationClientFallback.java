package com.cts.fundtrack.analytics.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;

/**
 * Circuit breaker fallback for {@link ApplicationClient}.
 *
 * <p>Activated when the Application Service is unreachable or returns repeated errors.
 * Returns safe empty/null defaults so analytics calculations can degrade gracefully
 * instead of propagating exceptions to callers.</p>
 */
@Component
public class ApplicationClientFallback implements ApplicationClient {

    private static final Logger log = LoggerFactory.getLogger(ApplicationClientFallback.class);

    /**
     * Returns an empty list when the Application Service is unavailable.
     * Analytics calculations that depend on this data will produce zero counts
     * rather than throwing exceptions.
     *
     * @param programId the program whose applications were requested
     * @return an empty list
     */
    @Override
    public List<ApplicationResponseDTO> getApplicationsByProgram(UUID programId) {
        log.warn("[CircuitBreaker] Application Service unavailable — returning empty applications for programId={}", programId);
        return Collections.emptyList();
    }

    /**
     * Returns {@code null} when the Application Service is unavailable.
     * Callers must null-check this return value before use.
     *
     * @param programId the program whose details were requested
     * @return {@code null}
     */
    @Override
    public ProgramResponseDTO getProgramDetails(UUID programId) {
        log.warn("[CircuitBreaker] Application Service unavailable — returning null program details for programId={}", programId);
        return null;
    }
}
