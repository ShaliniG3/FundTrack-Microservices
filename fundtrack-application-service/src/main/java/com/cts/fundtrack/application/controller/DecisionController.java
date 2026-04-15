package com.cts.fundtrack.application.controller;

import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.application.service.DecisionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    /**
     * Fetch list for Approver Console.
     * Accessible by Approvers and Admins.
     */
    @GetMapping("/pending/under-review")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponseDTO>> getUnderReview() {
        return ResponseEntity.ok(decisionService.getApplicationsUnderReview());
    }

    /**
     * View specific application details (including reviews/recommendations).
     * Accessible by Approvers and Admins.
     */
    @GetMapping("/application/{id}")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<ApplicationDecisionDetailsDTO> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(decisionService.getApplicationById(id));
    }

    /**
     * Submit final sign-off (APPROVE/REJECT).
     * Strictly restricted to the 'APPROVER' role.
     */
    @PostMapping("/process")
    @PreAuthorize("hasRole('APPROVER')")
    public ResponseEntity<String> processDecision(@RequestBody DecisionRequestDTO dto) {
        decisionService.processDecision(dto);
        return ResponseEntity.ok("Grant application has been marked as: " + dto.getDecision());
    }

    /**
     * View history of decisions made by a specific approver.
     * Approvers can view their own; Admins can view any.
     */
    @GetMapping("/approver/{approverId}")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<ApproverDashBoardDTO> getDecisionsByApproverId(@PathVariable UUID approverId) {
        return ResponseEntity.ok(decisionService.getDecisionsByApprover(approverId));
    }

    /**
     * Revert a decision.
     * This is a high-level override, typically restricted to Admins.
     */
    @DeleteMapping("/application/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDecision(@PathVariable UUID applicationId) {
        decisionService.deleteDecisionByApplicationId(applicationId);
        return ResponseEntity.ok("Decision deleted and Application reverted to UNDER_REVIEW for ID: " + applicationId);
    }
}