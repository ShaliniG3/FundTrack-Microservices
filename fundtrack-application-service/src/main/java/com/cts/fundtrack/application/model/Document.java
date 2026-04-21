package com.cts.fundtrack.application.model;

import java.util.UUID;

import com.cts.fundtrack.common.models.enums.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a supporting document uploaded by an applicant as part
 * of their grant application in the FundTrack system.
 *
 * <p>Documents are owned by an {@link Application} through a many-to-one
 * relationship and are cascade-deleted when their parent application is removed.
 * Only file types with extensions {@code pdf}, {@code jpg}, {@code jpeg}, and
 * {@code png} are accepted during submission; others are rejected by the service
 * layer before a {@code Document} record is ever created.</p>
 *
 * <p>The {@code verificationStatus} is managed by the Decision Service: it is set
 * to {@code SUBMITTED} on upload, updated to {@code DOCUMENT_APPROVED} or
 * {@code DOCUMENT_REJECTED} when a final funding decision is processed, and reset
 * to {@code SUBMITTED} if a decision is revoked.</p>
 */
@Entity
@Table(name = "documents")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    /** Auto-generated UUID primary key for this document record. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID documentId;

    /**
     * The parent application to which this document belongs.
     * Excluded from JSON serialisation to prevent circular references when
     * the document is serialised as part of an application response.
     * Loaded lazily to avoid unnecessary joins.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    /**
     * A categorisation label for the document (e.g., {@code "PROOF_OF_INCOME"},
     * {@code "ID_DOCUMENT"}). Stored in upper-case as normalised by the service
     * layer; defaults to {@code "UNKNOWN"} if not provided by the applicant.
     */
    private String docType;

    /**
     * The URI or relative path pointing to the stored file.
     * MEDIUMTEXT (up to 16 MB) is required because documents are stored
     * as base64 data URIs (e.g., "data:image/jpeg;base64,...").
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String fileUri;

    /**
     * The current verification state of this document, managed by the approval
     * workflow. Progresses from {@code SUBMITTED} → {@code DOCUMENT_APPROVED}
     * or {@code DOCUMENT_REJECTED}.
     */
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
}