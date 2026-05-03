package com.cts.fundtrack.common.dto;

import java.util.UUID;

import com.cts.fundtrack.common.models.enums.VerificationStatus;

import lombok.Data;

/**
 * Data Transfer Object representing a single document attached to a grant application.
 *
 * <p>Documents are uploaded by applicants as supporting evidence (e.g., identity proof,
 * income certificate, project proposal). Each document carries a type key, a storage URI,
 * and a verification status that reflects whether the document has passed automated
 * or manual review.</p>
 */
@Data
public class DocumentDTO {

    /** Unique identifier of the document record. */
    private UUID documentId;

    /**
     * Type key identifying the category of document (e.g., {@code "ID_PROOF"},
     * {@code "INCOME_CERTIFICATE"}). Must match an entry in the program's required
     * documents list for the application to be considered complete.
     */
    private String docType;

    /** Secure URI or storage reference key pointing to the uploaded file. */
    private String fileUri;

    /**
     * Current verification state of this document.
     * Transitions from {@code SUBMITTED} to {@code DOCUMENT_APPROVED} or
     * {@code DOCUMENT_REJECTED} after review.
     */
    private VerificationStatus verificationStatus;
}