package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Data Transfer Object for partially updating an existing grant application.
 *
 * <p>Allows an applicant to revise their project narrative and/or replace/add
 * supporting documents before the application is submitted for review. Fields that
 * are {@code null} or empty are ignored by the service layer — only provided values
 * are applied to the persisted record.</p>
 *
 * <p>This DTO is typically used with an HTTP {@code PUT} or {@code PATCH} endpoint
 * and is only valid while the application is still in {@code DRAFT} or
 * {@code SUBMITTED} state.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUpdateDTO {

    /**
     * Revised free-text project description and eligibility data.
     * If {@code null}, the existing {@code applicationData} on the record is left unchanged.
     */
    private String applicationData;

    /**
     * Documents to add or replace on the application.
     * If {@code null} or empty, the existing document list is left unchanged.
     */
    private List<DocumentDTO> documents;
}