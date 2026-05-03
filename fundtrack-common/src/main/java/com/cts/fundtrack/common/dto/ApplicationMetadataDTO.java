package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight Data Transfer Object carrying the essential identifying information
 * about a grant application.
 *
 * <p>Used in listing and summary views where the full {@link ApplicationResponseDTO}
 * payload would be excessive — for example, in notification templates, audit log
 * references, or dashboard row items that only need applicant/program identification
 * and the current status.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationMetadataDTO {

    /** Unique identifier of the application record. */
    private UUID applicationId;

    /** Unique identifier of the user who submitted the application. */
    private UUID applicantUserId;

    /** Display name of the applicant (resolved from the Identity Service). */
    private String applicantName;

    /** Human-readable name of the funding program this application targets. */
    private String programName;

    /** Current lifecycle status of the application (e.g., "SUBMITTED", "APPROVED"). */
    private String status;
}