package com.fundtrack.modules.decision_service.controller;

import com.fundtrack.modules.application_service.dto.ApplicationResponseDTO;
import com.fundtrack.modules.application_service.models.Application;
import com.fundtrack.modules.decision_service.dto.ApplicationDecisionDetailsDTO;
import com.fundtrack.modules.decision_service.dto.ApproverDashBoardDTO;
import com.fundtrack.modules.decision_service.dto.DecisionRequestDTO;
import com.fundtrack.modules.decision_service.service.DecisionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    // Fetch list for Approver Console
    @GetMapping("/pending/under-review")
    public ResponseEntity<List<ApplicationResponseDTO>> getUnderReview() {
        return ResponseEntity.ok(decisionService.getApplicationsUnderReview());
    }

    // View specific application details before decision
    @GetMapping("/application/{id}")
    public ResponseEntity<ApplicationDecisionDetailsDTO> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(decisionService.getApplicationById(id));
    }

    // Submit final sign-off
    @PostMapping("/process")
    public ResponseEntity<String> processDecision(@RequestBody DecisionRequestDTO dto) {
        decisionService.processDecision(dto);
        return ResponseEntity.ok("Grant application has been marked as: " + dto.getDecision());
    }

    @GetMapping("/approver/{approverId}")
    public ResponseEntity<ApproverDashBoardDTO> getDecisionsByApproverId(@PathVariable UUID approverId) {
        return ResponseEntity.ok(decisionService.getDecisionsByApprover(approverId));
    }

    @DeleteMapping("/application/{applicationId}")
    public ResponseEntity<String> deleteDecision(@PathVariable UUID applicationId) {
        decisionService.deleteDecisionByApplicationId(applicationId);
        return ResponseEntity.ok("Decision deleted and Application reverted to UNDER_REVIEW for ID: " + applicationId);
    }
}