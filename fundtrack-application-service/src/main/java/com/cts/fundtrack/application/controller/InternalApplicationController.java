package com.cts.fundtrack.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.application.service.ApplicationService;
import com.cts.fundtrack.common.dto.ApplicationMetadataDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal REST controller exposing application lifecycle operations for
 * consumption by peer microservices (e.g. the Disbursement Service) via Feign.
 *
 * <p>Base path: {@code /api/internal}</p>
 *
 * <p>These endpoints are NOT routed through the API Gateway to external clients.
 * They are called service-to-service with gateway-propagated security headers,
 * so no additional {@code @PreAuthorize} constraints are needed beyond the
 * general authentication requirement configured in {@code SecurityConfig}.</p>
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/internal/programs/{programId}/winners} — UUIDs of all
 *       APPROVED applications for a program; consumed during budget finalization.</li>
 *   <li>{@code GET /api/internal/programs/{programId}/has-pending} — guard check
 *       that blocks finalization if any application is still SUBMITTED or UNDER_REVIEW.</li>
 *   <li>{@code PUT /api/internal/applications/{id}/status/{newStatus}} — moves an
 *       application to a new lifecycle status (e.g., ACCEPTED after disbursement
 *       schedule creation).</li>
 *   <li>{@code GET /api/internal/applications/{id}/metadata} — lightweight metadata
 *       for display and audit use by peer services.</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApplicationController {

    private final ApplicationService applicationService;

    /**
     * Returns the UUIDs of all APPROVED applications for a given program.
     * The Disbursement Service uses this to determine which applicants receive
     * a budget share during the finalization workflow.
     *
     * @param programId the UUID of the grant program
     * @return list of approved application UUIDs; empty list if none exist
     */
    @GetMapping("/programs/{programId}/winners")
    public ResponseEntity<List<UUID>> getApprovedApplicationIds(@PathVariable UUID programId) {
        log.debug("Internal request: approved application IDs for program {}", programId);
        return ResponseEntity.ok(applicationService.getApprovedApplicationIds(programId));
    }

    /**
     * Returns {@code true} if any application for the given program is still in
     * {@code SUBMITTED} or {@code UNDER_REVIEW} status, blocking premature
     * budget finalization.
     *
     * @param programId the UUID of the program to check
     * @return {@code true} if pending reviews exist; {@code false} if all reviewed
     */
    @GetMapping("/programs/{programId}/has-pending")
    public ResponseEntity<Boolean> hasPendingReviews(@PathVariable UUID programId) {
        log.debug("Internal request: pending review check for program {}", programId);
        return ResponseEntity.ok(applicationService.hasPendingReviews(programId));
    }

    /**
     * Updates the lifecycle status of a specific application.
     * Called by the Disbursement Service to transition an application to
     * {@code ACCEPTED} once its installment schedule has been created.
     *
     * @param id        the UUID of the application to update
     * @param newStatus the target status string (e.g., {@code "ACCEPTED"})
     * @return HTTP 200 with no body on success
     */
    @PutMapping("/applications/{id}/status/{newStatus}")
    public ResponseEntity<Void> updateApplicationStatus(
            @PathVariable UUID id,
            @PathVariable String newStatus) {
        log.info("Internal request: updating application {} to status {}", id, newStatus);
        applicationService.updateApplicationStatus(id, newStatus);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns lightweight metadata for a specific application, used by peer services
     * for display and audit purposes.
     *
     * @param id the UUID of the application
     * @return an {@link ApplicationMetadataDTO} with applicant ID, status, and
     *         available name fields
     */
    @GetMapping("/applications/{id}/metadata")
    public ResponseEntity<ApplicationMetadataDTO> getApplicationMetadata(@PathVariable UUID id) {
        log.debug("Internal request: metadata for application {}", id);
        return ResponseEntity.ok(applicationService.getApplicationMetadata(id));
    }
}
