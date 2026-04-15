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

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * 1. POST: Unified Application Submission
     * Only Applicants are allowed to submit new grant requests.
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
     * 2. PUT: Update Application
     * Applicants can update their submission while it's in draft/submitted state.
     */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponseDTO> updateApplication(
            @PathVariable UUID id,
            @RequestBody ApplicationUpdateDTO dto) {
        return ResponseEntity.ok(applicationService.updateApplication(id, dto));
    }

    /**
     * 3. GET: Full Dashboard Details
     * Accessible by the Applicant (owner) and Reviewers/Admins.
     */
    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('APPLICANT', 'APPROVER', 'ADMIN')")
    public ResponseEntity<ApplicantDetailsDTO> getFullApplicationDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getFullApplicationDetails(id));
    }

    /**
     * 4. GET: Documents
     * Restricted to Reviewers and Admins for the decision-making process.
     */
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ResponseEntity<List<DocumentDTO>> getDocumentsByApplicationId(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getDocumentsByApplicationId(id));
    }

    /**
     * 5. GET: Validation Results
     * Applicants view this to see why they failed; Approvers view it to confirm eligibility.
     */
    @GetMapping("/{id}/validation")
    @PreAuthorize("hasAnyRole('APPLICANT', 'APPROVER', 'ADMIN')")
    public ResponseEntity<List<ValidationResultDTO>> getValidationResults(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getValidationResults(id));
    }

    /**
     * 7. DELETE: Withdraw Application
     * Only the Applicant can withdraw their own application.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}