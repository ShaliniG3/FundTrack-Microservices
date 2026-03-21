package com.fundtrack.analytics_service.dto.documentdto;

import com.fundtrack.analytics_service.model.external.DocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing a document uploaded for a grant application.
 * <p>
 * This DTO contains metadata for identifying the document type, its storage location
 * (URI), and its current verification state within the application workflow.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata for a supporting document uploaded as part of a grant application")
public class DocumentDTO {

    /**
     * Unique identifier for the document record.
     */
    @Schema(description = "The unique ID of the document", example = "c3d4e5f6-g7h8-9i0j-k1l2-m3n4o5p6q7r8")
    private UUID documentID;

    /**
     * Unique identifier of the application this document is attached to.
     */
    @Schema(description = "The ID of the application this document belongs to", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationID;

    /**
     * The category or type of the document.
     * <p>Examples: ID_PROOF, INCOME_TAX_RETURN, PROJECT_PROPOSAL.</p>
     */
    @Schema(description = "The type of document provided", example = "PROJECT_PROPOSAL")
    private String docType;

    /**
     * The resource identifier or URL where the file is stored.
     * <p>This could be an S3 bucket key, a Cloud Storage URL, or a local file path.</p>
     */
    @Schema(description = "The storage URI or download link for the file", example = "s3://fundtrack-docs/apps/123/proposal.pdf")
    private String fileURI;

    /**
     * The timestamp when the document was successfully uploaded.
     */
    @Schema(description = "The date and time the document was uploaded")
    private Instant uploadedDate;

    /**
     * The current status of the document's verification process.
     * <p>Expected values: PENDING, VERIFIED, REJECTED.</p>
     */
    @Schema(description = "The verification status of the document", example = "VERIFIED")
    private DocumentStatus verificationStatus;
}