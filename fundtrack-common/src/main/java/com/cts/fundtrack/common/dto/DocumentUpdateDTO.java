package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for adding or replacing a document on an existing application.
 *
 * <p>Used within an {@link ApplicationUpdateDTO} to describe individual document changes.
 * If {@code documentId} is provided, the service treats the entry as an update to an
 * existing document record; if omitted, a new document record is created.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateDTO {

    /**
     * Optional identifier of an existing document to update.
     * If {@code null}, a new document record will be created instead.
     */
    private UUID documentId;

    /**
     * Type key identifying the category of document (e.g., {@code "ID_PROOF"},
     * {@code "INCOME_CERTIFICATE"}).
     */
    private String docType;

    /** Secure URI or storage reference key pointing to the uploaded file. */
    private String fileUri;
}