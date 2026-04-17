package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.dto.ProgramMetadataDTO; // Imported from common
import com.cts.fundtrack.common.config.FeignConfig; // Imported from common

/**
 * Feign client for communicating with the FundTrack Program Service.
 * <p>
 * Provides internal service-to-service access to program metadata — specifically
 * the program budget and lifecycle status — required during the budget-splitting
 * and disbursement finalization workflow. Uses the shared {@link FeignConfig} for
 * authentication header propagation and falls back to {@link ProgramFallback} when
 * the Program Service is unavailable.
 * </p>
 */
@FeignClient(
    name = "fundtrack-program-service",
    configuration = FeignConfig.class,
    fallback = ProgramFallback.class
)
public interface ProgramClient {

    /**
     * Retrieves metadata for a specific grant program, including its total budget,
     * name, and current lifecycle status.
     * <p>
     * The disbursement service uses this to validate that the program is in
     * {@code CLOSED} status before initiating a budget split, and to obtain the
     * total budget for equal distribution among approved applicants.
     * </p>
     *
     * @param id the UUID of the grant program to retrieve
     * @return a {@link ProgramMetadataDTO} with the program's budget, name, and status;
     *         returns a stub with {@code status = "SERVICE_UNAVAILABLE"} and
     *         {@code budget = -1.0} on fallback
     */
    @GetMapping("/api/internal/programs/{id}")
    ProgramMetadataDTO getProgramById(@PathVariable("id") UUID id);
}