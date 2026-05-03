package com.cts.fundtrack.disbursement.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.cts.fundtrack.common.config.FeignConfig; // 1. IMPORT FROM COMMON
import com.cts.fundtrack.common.dto.ApplicationMetadataDTO;

/**
 * Feign client for communicating with the FundTrack Application Service.
 * <p>
 * This client provides internal service-to-service access to application metadata
 * and lifecycle operations required during the disbursement finalization workflow.
 * It uses the shared {@link FeignConfig} for authentication header propagation and
 * falls back to {@link ApplicationFallback} when the Application Service is unavailable.
 * </p>
 */
@FeignClient(
    name = "fundtrack-application-service",
    configuration = FeignConfig.class,
    fallback = ApplicationFallback.class
)
public interface ApplicationClient {

    /**
     * Retrieves metadata for a specific grant application.
     *
     * @param id the UUID of the target application
     * @return an {@link ApplicationMetadataDTO} containing the applicant name, status,
     *         and other summary fields; returns a stub with {@code "UNKNOWN"} status on fallback
     */
    @GetMapping("/api/internal/applications/{id}/metadata")
    ApplicationMetadataDTO getApplicationMetadata(@PathVariable("id") UUID id);

    /**
     * Retrieves the UUIDs of all approved (winning) applications for a given program.
     * <p>
     * Used during the budget-splitting phase to determine which applicants receive
     * disbursement schedules.
     * </p>
     *
     * @param programId the UUID of the grant program
     * @return a list of approved application UUIDs; returns an empty list on fallback
     */
    @GetMapping("/api/internal/programs/{programId}/winners")
    List<UUID> getApprovedApplicationIds(@PathVariable("programId") UUID programId);

    /**
     * Checks whether any applications in a program still have pending review decisions.
     * <p>
     * Used as a pre-condition guard before budget splitting — if any applications are
     * still under review, the split is blocked. Defaults to {@code true} (safe-block)
     * on fallback to prevent premature finalization.
     * </p>
     *
     * @param programId the UUID of the grant program to check
     * @return {@code true} if at least one application is still pending review;
     *         {@code false} if all reviews are complete; {@code true} on fallback
     */
    @GetMapping("/api/internal/programs/{programId}/has-pending")
    Boolean hasPendingReviews(@PathVariable("programId") UUID programId);

    /**
     * Updates the lifecycle status of a specific grant application.
     * <p>
     * Called after a disbursement schedule is successfully created to transition
     * the application to {@code ACCEPTED} status.
     * </p>
     *
     * @param id        the UUID of the application to update
     * @param newStatus the target status string (e.g., {@code "ACCEPTED"})
     */
    @PutMapping("/api/internal/applications/{id}/status/{newStatus}")
    void updateApplicationStatus(@PathVariable("id") UUID id, @PathVariable("newStatus") String newStatus);
}