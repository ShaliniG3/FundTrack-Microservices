package com.fundtrack.modules.application_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fundtrack.modules.application_service.dto.*;
import com.fundtrack.modules.application_service.service.ApplicationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;



    /**
     * 1. POST: Unified Application Submission
     * Requirement 2.3 & 4.3: Submit data and documents in one call.
     * Note: applicantId is taken from Header to simulate a secure login session.
     */
    @PostMapping("/submit")
    public ResponseEntity<ApplicationResponseDTO> applyToProgram(
            @RequestHeader("X-Applicant-Id") UUID applicantId,
            @RequestBody ApplicationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.applyToProgram(applicantId, dto));
    }




    /**
     * 2. PUT: Update Application
     * Requirement 1.2: Support for iterative submission.
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApplicationResponseDTO> updateApplication(
            @PathVariable UUID id,
            @RequestBody ApplicationUpdateDTO dto) {
        return ResponseEntity.ok(applicationService.updateApplication(id, dto));
    }



    /**
     * 3. GET: Full Dashboard Details
     * Requirement 7.1: Provide transparency for the applicant.
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<ApplicantDetailsDTO> getFullApplicationDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getFullApplicationDetails(id));
    }



    /**
     * 4. GET: Documents (For Decision Module)
     * Requirement 4.5: Internal/External API to fetch docs for Approvers.
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentDTO>> getDocumentsByApplicationId(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getDocumentsByApplicationId(id));
    }



    /**
     * 5. GET: Validation Results
     * Requirement 4.3: View results of SpEL eligibility checks.
     */
    @GetMapping("/{id}/validation")
    public ResponseEntity<List<ValidationResultDTO>> getValidationResults(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getValidationResults(id));
    }




    /**
     * 7. DELETE: Withdraw Application
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}