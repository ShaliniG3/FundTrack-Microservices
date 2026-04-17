package com.cts.fundtrack.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.application.service.ApplicationService;
import com.cts.fundtrack.common.dto.ApplicantDetailsDTO;
import com.cts.fundtrack.common.dto.ApplicationRequestDTO;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ApplicationUpdateDTO;
import com.cts.fundtrack.common.dto.DocumentDTO;
import com.cts.fundtrack.common.dto.ValidationResultDTO;

import lombok.RequiredArgsConstructor;

/**
 * REST controller that exposes grant application lifecycle endpoints for the
 * FundTrack Application Service.
 *
 * <p>Base path: {@code /api/v1/applications}</p>
 *
 * <p>Handles the full lifecycle of a grant application from initial submission
 * through updates, document retrieval, validation result inspection, and
 * withdrawal. Role-based access control is enforced at the method level via
 * {@code @PreAuthorize} annotations, with the acting user's identity and role
 * injected by the API Gateway through trusted HTTP headers.</p>
 *
 * <p>Roles used in this controller:
 * <ul>
 *   <li>{@code APPLICANT} — submits, updates, views and withdraws their own applications</li>
 *   <li>{@code REVIEWER} — reads application summaries and validation results</li>
 *   <li>{@code APPROVER} — reads summaries, documents, and validation results</li>
 *   <li>{@code ADMIN} — has full read access across all endpoints</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * Submits a new grant application on behalf of the authenticated applicant.
     *
     * <p>The applicant's UUID is extracted from the {@code X-User-Id} header
     * injected by the API Gateway. A duplicate-application check is performed
     * before persisting; automated eligibility validation runs immediately
     * after submission and may set the status to {@code REJECTED} if rules
     * are not met.</p>
     *
     * @param applicantId the UUID of the authenticated applicant, sourced from
     *                    the {@code X-User-Id} gateway header
     * @param dto         the application payload containing the target program ID,
     *                    freeform application data, and optional supporting documents
     * @return a {@link ResponseEntity} with HTTP 201 and the persisted
     *         {@link ApplicationResponseDTO} including generated ID and initial status
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponseDTO> applyToProgram(
            @RequestHeader("X-User-Id") UUID applicantId,
            @RequestBody ApplicationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.applyToProgram(applicantId, dto));
    }

    /**
     * Updates an existing grant application with new data provided by the applicant.
     *
     * <p>Updates are only permitted while the application is in {@code SUBMITTED}
     * or {@code UNDER_REVIEW} state. Providing new {@code applicationData} resets
     * the status to {@code SUBMITTED} and re-triggers automated eligibility
     * validation.</p>
     *
     * @param id  the UUID of the application to update
     * @param dto the update payload containing the new application data and/or
     *            document list
     * @return a {@link ResponseEntity} with HTTP 200 and the updated
     *         {@link ApplicationResponseDTO}
     */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponseDTO> updateApplication(
            @PathVariable UUID id,
            @RequestBody ApplicationUpdateDTO dto) {
        return ResponseEntity.ok(applicationService.updateApplication(id, dto));
    }

    /**
     * Retrieves the full details of a specific grant application for dashboard display.
     *
     * <p>Returns a comprehensive view including application data, current status,
     * associated documents, and validation results. Accessible by the owning
     * applicant as well as reviewers, approvers, and administrators.</p>
     *
     * @param id the UUID of the application to retrieve
     * @return a {@link ResponseEntity} with HTTP 200 and the {@link ApplicantDetailsDTO}
     *         containing all application fields and related collections
     */
    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('APPLICANT', 'APPROVER', 'ADMIN', 'REVIEWER')")
    public ResponseEntity<ApplicantDetailsDTO> getFullApplicationDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getFullApplicationDetails(id));
    }

    /**
     * Retrieves all documents attached to a specific grant application.
     *
     * <p>Documents are used by approvers and administrators during the
     * decision-making process to verify the applicant's supporting evidence.
     * Access is restricted to the {@code APPROVER} and {@code ADMIN} roles.</p>
     *
     * @param id the UUID of the application whose documents are requested
     * @return a {@link ResponseEntity} with HTTP 200 and a list of
     *         {@link DocumentDTO} objects, each containing the document type,
     *         file URI, and verification status
     */
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<List<DocumentDTO>> getDocumentsByApplicationId(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getDocumentsByApplicationId(id));
    }

    /**
     * Retrieves the automated eligibility validation results for a specific application.
     *
     * <p>Each result indicates whether the applicant's data satisfied a specific
     * program eligibility rule evaluated using SpEL expressions. Applicants use
     * this endpoint to understand the reason for an automated rejection; approvers
     * use it to confirm eligibility before issuing a decision.</p>
     *
     * @param id the UUID of the application whose validation results are requested
     * @return a {@link ResponseEntity} with HTTP 200 and a list of
     *         {@link ValidationResultDTO} objects showing per-rule pass/fail outcomes
     */
    @GetMapping("/{id}/validation")
    @PreAuthorize("hasAnyRole('APPLICANT', 'APPROVER', 'ADMIN', 'REVIEWER')")
    public ResponseEntity<List<ValidationResultDTO>> getValidationResults(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getValidationResults(id));
    }

    /**
     * Withdraws (permanently deletes) a grant application submitted by the applicant.
     *
     * <p>Only the {@code APPLICANT} role may withdraw applications. All associated
     * documents and validation records are cascade-deleted. A confirmation
     * notification is dispatched to the applicant upon successful withdrawal.</p>
     *
     * @param id the UUID of the application to delete
     * @return a {@link ResponseEntity} with HTTP 204 and no body
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all grant applications submitted by the currently authenticated applicant.
     *
     * <p>The applicant's UUID is sourced from the {@code X-User-Id} header injected
     * by the API Gateway based on the validated JWT token. This endpoint supports
     * the "My Applications" dashboard view in the applicant-facing UI.</p>
     *
     * @param applicantId the UUID of the authenticated applicant, sourced from
     *                    the {@code X-User-Id} gateway header
     * @return a {@link ResponseEntity} with HTTP 200 and a list of
     *         {@link ApplicationResponseDTO} objects for all applications belonging
     *         to the specified applicant
     */
    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<List<ApplicationResponseDTO>> getMyApplications(
            @RequestHeader("X-User-Id") UUID applicantId) {
        return ResponseEntity.ok(applicationService.getMyApplications(applicantId));
    }
}