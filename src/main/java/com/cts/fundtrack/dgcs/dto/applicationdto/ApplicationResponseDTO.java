package com.cts.fundtrack.dgcs.dto.applicationdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lean response object for cross-service enrichment")
public class ApplicationResponseDTO {

    private UUID applicationId;
    private UUID programId;
    private UUID userId;

    @Schema(example = "John Doe")
    private String userName;

    @Schema(example = "Education Grant 2026")
    private String programName;

    // We use String here so we don't have to import the ApplicationStatus Enum
    private String status;

    private Instant submittedDate;

    // Note: Documents and Validations are removed because
    // the Compliance Service doesn't need them for the dashboard.
}