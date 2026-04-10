package com.cts.fundtrack.dgcs.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.UUID;

/**
 * Data Transfer Object for cross-service application metadata.
 * <p>
 * This DTO is used by Feign clients to fetch read-only snapshots of an
 * application's core details from the Application Service. It provides
 * context for disbursement installments and compliance reviews.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Metadata snapshot of a grant application retrieved from the Application Service.")
public class ApplicationMetadataDTO {

    @Schema(description = "The unique identifier of the application",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID applicationId;

    @Schema(description = "The unique identifier of the user who submitted the application",
            example = "b23a10b-58cc-4372-a567-0e02b2c3d479")
    private UUID applicantUserId;

    @Schema(description = "The display name of the applicant or organization",
            example = "Tech For Good Foundation")
    private String applicantName;

    @Schema(description = "The title of the grant program associated with this application",
            example = "Urban Sustainability Fund 2026")
    private String programName;

    @Schema(description = "The current lifecycle status of the application",
            example = "APPROVED")
    private String status;
}