package com.cts.fundtrack.application.controller;

import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.application.service.DecisionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that exposes decision management endpoints for the approver
 * workflow in the FundTrack grant funding system.
 *
 * <p>Base path: {@code /api/v1/decisions}</p>
 *
 * <p>This controller handles the final adjudication stage of the grant application
 * lifecycle. Once a reviewer has scored and recommended an application, an approver
 * uses these endpoints to issue, update, or revoke a binding {@code APPROVED} or
 * {@code REJECTED} decision. All mutations trigger applicant notifications and
 * audit log entries.</p>
 *
 * <p>Roles used in this controller:
 * <ul>
 *   <li>{@code APPROVER} — issues, updates, and reads decisions</li>
 *   <li>{@code ADMIN} — has full access to all endpoints including deletion</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    /**
     * Retrieves all grant applications currently in the {@code UNDER_REVIEW} state.
     *
     * <p>This endpoint populates the approver console work-queue, listing every
     * application that has been reviewed and is awaiting a final funding decision.</p>
     *
     * @return a {@link ResponseEntity} with HTTP 200 and a list of
     *         {@link ApplicationResponseDTO} objects representing applications
     *         awaiting a decision
     */
    @GetMapping("/pending/under-review")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponseDTO>> getUnderReview() {
        return ResponseEntity.ok(decisionService.getApplicationsUnderReview());
    }

    /**
     * Retrieves consolidated decision details for a specific application, including
     * the associated review score and reviewer recommendation.
     *
     * <p>Used by the approver to inspect all evaluator input before issuing a
     * final funding decision.</p>
     *
     * @param id the UUID of the application to retrieve
     * @return a {@link ResponseEntity} with HTTP 200 and an
     *         {@link ApplicationDecisionDetailsDTO} containing the application
     *         summary, review, and recommendation data
     */
    @GetMapping("/application/{id}")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<ApplicationDecisionDetailsDTO> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(decisionService.getApplicationById(id));
    }

    /**
     * Processes and persists a final funding decision ({@code APPROVED} or
     * {@code REJECTED}) for a grant application.
     *
     * <p>The application must be in the {@code UNDER_REVIEW} state before a
     * decision can be recorded. Upon success, the application status is updated,
     * all associated document verification statuses are set accordingly, and
     * both the applicant and the acting approver receive notifications.</p>
     *
     * @param dto the decision payload containing the application ID, approver ID,
     *            outcome string ({@code APPROVED} or {@code REJECTED}), and
     *            optional justification notes
     * @return a {@link ResponseEntity} with HTTP 200 and a confirmation message
     *         indicating the recorded decision outcome
     */
    @PostMapping("/process")
    @PreAuthorize("hasRole('APPROVER')")
    public ResponseEntity<String> processDecision(@RequestBody DecisionRequestDTO dto) {
        decisionService.processDecision(dto);
        return ResponseEntity.ok("Grant application has been marked as: " + dto.getDecision());
    }

    /**
     * Retrieves the complete decision history for a specific approver, supporting
     * performance metrics and workload audits.
     *
     * <p>An approver may view their own decision history; an admin may query any
     * approver's history.</p>
     *
     * @param approverId the UUID of the approver whose decision history is requested
     * @return a {@link ResponseEntity} with HTTP 200 and an
     *         {@link ApproverDashBoardDTO} containing the total decision count and
     *         a list of individual {@link com.cts.fundtrack.common.dto.DecisionDTO} entries
     */
    @GetMapping("/approver/{approverId}")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<ApproverDashBoardDTO> getDecisionsByApproverId(@PathVariable UUID approverId) {
        return ResponseEntity.ok(decisionService.getDecisionsByApprover(approverId));
    }

    /**
     * Updates the outcome or justification notes of an existing decision for a
     * specific application.
     *
     * <p>Allows an approver or admin to correct an erroneous decision (e.g., flip
     * from {@code APPROVED} to {@code REJECTED}) or amend the decision notes. The
     * application status and document verification statuses are updated to reflect
     * the new outcome.</p>
     *
     * @param applicationId the UUID of the application whose decision is to be updated
     * @param dto           the updated decision payload with the new outcome and/or notes
     * @return a {@link ResponseEntity} with HTTP 200 and a confirmation message
     */
    @PatchMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<String> updateDecision(
            @PathVariable UUID applicationId,
            @RequestBody DecisionRequestDTO dto) {
        decisionService.updateDecision(applicationId, dto);
        return ResponseEntity.ok("Decision updated for application ID: " + applicationId);
    }

    /**
     * Deletes the decision record for a specific application and reverts the
     * application status to {@code UNDER_REVIEW}.
     *
     * <p>This is an administrative correction operation. All document verification
     * statuses are reset to {@code SUBMITTED}. Both the applicant and the acting
     * staff member receive notifications indicating that the decision has been
     * revoked and the application is back under review.</p>
     *
     * @param applicationId the UUID of the application whose decision is to be deleted
     * @return a {@link ResponseEntity} with HTTP 200 and a confirmation message
     */
    @DeleteMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<String> deleteDecision(@PathVariable UUID applicationId) {
        decisionService.deleteDecisionByApplicationId(applicationId);
        return ResponseEntity.ok("Decision deleted and Application reverted to UNDER_REVIEW for ID: " + applicationId);
    }
}