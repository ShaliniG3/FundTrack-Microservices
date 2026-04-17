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
 * Feign client for communicating with the {@code fundtrack-application-service}.
 *
 * <p>Provides read access to grant applications and funding program details that
 * the Analytics Service needs to compute status distributions, daily trends, and
 * financial summaries.</p>
 *
 * <p>Header propagation (e.g., {@code X-User-Id}, {@code X-User-Role}) is handled
 * automatically by the shared {@link FeignConfig} request interceptor. If the
 * downstream service is unavailable, {@link ApplicationClientFallback} is activated
 * by the circuit breaker.</p>
 */
@FeignClient(
    name = "fundtrack-application-service",
    configuration = FeignConfig.class,
    fallback = ApplicationClientFallback.class
)
public interface ApplicationClient {

    /**
     * Retrieves all grant applications associated with a specific funding program.
     *
     * @param programId the unique identifier of the program whose applications are requested
     * @return a {@link List} of {@link ApplicationResponseDTO} objects; never {@code null},
     *         but may be empty if no applications exist or the fallback is triggered
     */
    @GetMapping("/api/v1/applications/program/{programId}")
    List<ApplicationResponseDTO> getApplicationsByProgram(@PathVariable("programId") UUID programId);

    /**
     * Retrieves the metadata and configuration details for a specific funding program.
     *
     * <p>Used by the analytics engine to obtain the program's total budget for
     * utilization calculations.</p>
     *
     * @param programId the unique identifier of the program to look up
     * @return the {@link ProgramResponseDTO} for the requested program,
     *         or {@code null} if the fallback is triggered
     */
    @GetMapping("/api/v1/programs/{programId}")
    ProgramResponseDTO getProgramDetails(@PathVariable("programId") UUID programId);
}