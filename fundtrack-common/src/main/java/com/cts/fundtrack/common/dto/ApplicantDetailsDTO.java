package com.cts.fundtrack.common.dto;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.ApplicationStatus;

import lombok.Data;

/**
 * Data Transfer Object providing a full view of an applicant's submitted grant application.
 *
 * <p>Returned to authenticated applicants so they can review their own submission
 * state, uploaded documents, and the results of any automated or manual validation
 * checks that have been run against their application.</p>
 */
@Data
public class ApplicantDetailsDTO {

    /** Unique identifier of the application record. */
    private UUID applicationId;

    /** Unique identifier of the funding program this application targets. */
    private UUID programId;

    /** Current lifecycle status of the application (e.g., SUBMITTED, UNDER_REVIEW). */
    private ApplicationStatus status;

    /** Free-text project description and eligibility data submitted by the applicant. */
    private String applicationData;

    /** Timestamp recording when the application was first created in the system. */
    private LocalDateTime createdAt;

    /** List of documents uploaded as supporting evidence for this application. */
    private List<DocumentDTO> documents;

    /** List of validation results produced by automated or manual eligibility checks. */
    private List<ValidationResultDTO> validationResults;
}