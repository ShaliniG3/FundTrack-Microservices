package com.cts.fundtrack.application.mapper;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.application.model.ApplicationValidation;
import com.cts.fundtrack.application.model.Document;
import com.cts.fundtrack.common.dto.ApplicantDetailsDTO;
import com.cts.fundtrack.common.dto.ApplicationRequestDTO;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.DocumentDTO;
import com.cts.fundtrack.common.dto.ValidationDTO;
import com.cts.fundtrack.common.dto.ValidationResultDTO;

/**
 * Manual Spring-managed mapper that converts between {@link Application} JPA entities
 * and the various DTOs used across the FundTrack Application Service API surface.
 *
 * <p>This class is intentionally hand-written rather than generated (e.g., by MapStruct)
 * to allow fine-grained control over field-level mapping logic, particularly for
 * nested collections ({@link Document}, {@link ApplicationValidation}) and timestamp
 * conversions between {@link java.time.LocalDateTime} and {@link java.time.Instant}.</p>
 *
 * <p>Mapping responsibilities:
 * <ul>
 *   <li>{@link ApplicationRequestDTO} → {@link Application} entity (inbound submission)</li>
 *   <li>{@link Application} → {@link ApplicationResponseDTO} (standard API response)</li>
 *   <li>{@link Application} → {@link ApplicantDetailsDTO} (full dashboard view)</li>
 *   <li>{@link Document} → {@link DocumentDTO} (document list response)</li>
 *   <li>{@link ApplicationValidation} → {@link ValidationResultDTO} (validation detail response)</li>
 * </ul>
 * </p>
 */
@Component
public class ApplicationMapper {

    /**
     * Converts an inbound {@link ApplicationRequestDTO} to a new {@link Application} entity.
     *
     * <p>Only {@code programId} and {@code applicationData} are copied from the DTO;
     * the {@code applicantId} is intentionally left unset and must be assigned in the
     * service layer from the authenticated request context.</p>
     *
     * @param dto the request DTO received from the client; may be {@code null}
     * @return a partially populated {@link Application} entity, or {@code null} if
     *         {@code dto} is {@code null}
     */
    public Application toEntity(ApplicationRequestDTO dto) {
        if (dto == null) return null;
        Application entity = new Application();
        entity.setProgramId(dto.getProgramId());
        entity.setApplicationData(dto.getApplicationData());
        // applicantId is set in the Service layer
        return entity;
    }

    /**
     * Converts an {@link Application} entity to the standard {@link ApplicationResponseDTO}
     * used as the API response for submission and update operations.
     *
     * <p>The {@code submittedDate} is derived from {@code createdAt} and converted from
     * {@link java.time.LocalDateTime} to {@link java.time.Instant} using UTC offset.
     * Associated documents and validation records are mapped to their respective DTO
     * representations via private helper methods.</p>
     *
     * @param entity the {@link Application} entity to convert; may be {@code null}
     * @return a fully populated {@link ApplicationResponseDTO}, or {@code null} if
     *         {@code entity} is {@code null}
     */
    public ApplicationResponseDTO toResponseDTO(Application entity) {
        if (entity == null) return null;

        return ApplicationResponseDTO.builder()
                .applicationId(entity.getApplicationId())
                .programId(entity.getProgramId())
                .userId(entity.getApplicantId())
                .status(entity.getStatus())
                .submittedDate(entity.getCreatedAt() != null 
                        ? entity.getCreatedAt().toInstant(ZoneOffset.UTC) 
                        : null)
                .documents(mapDocuments(entity.getDocuments()))
                .validations(mapValidationsToDTO(entity.getValidations()))
                .build();
    }

    /**
     * Converts an {@link Application} entity to the {@link ApplicantDetailsDTO} used
     * by the full application summary endpoint ({@code GET /applications/{id}/summary}).
     *
     * <p>Unlike {@link #toResponseDTO(Application)}, this mapping includes the raw
     * {@code applicationData} string and the {@code createdAt} timestamp as a
     * {@link java.time.LocalDateTime}, providing a richer view suitable for dashboard
     * display. Nested documents and validation results are each mapped to their own
     * DTO lists.</p>
     *
     * @param entity the {@link Application} entity to convert; may be {@code null}
     * @return a fully populated {@link ApplicantDetailsDTO}, or {@code null} if
     *         {@code entity} is {@code null}
     */
    public ApplicantDetailsDTO toFullDetailsDTO(Application entity) {
        if (entity == null) return null;

        ApplicantDetailsDTO dto = new ApplicantDetailsDTO();
        dto.setApplicationId(entity.getApplicationId());
        dto.setProgramId(entity.getProgramId());
        dto.setApplicationData(entity.getApplicationData());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getDocuments() != null) {
            dto.setDocuments(entity.getDocuments().stream()
                    .map(this::toDocumentDTO)
                    .collect(Collectors.toList()));
        }

        if (entity.getValidations() != null) {
            dto.setValidationResults(entity.getValidations().stream()
                    .map(this::toValidationDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Converts a single {@link Document} entity to a {@link DocumentDTO}.
     *
     * <p>This method is also called directly from the service layer when
     * stream-mapping document collections, making it {@code public} rather
     * than private.</p>
     *
     * @param entity the {@link Document} entity to convert; may be {@code null}
     * @return a {@link DocumentDTO} containing the document ID, type, file URI,
     *         and verification status, or {@code null} if {@code entity} is {@code null}
     */
    public DocumentDTO toDocumentDTO(Document entity) {
        if (entity == null) return null;
        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentId(entity.getDocumentId());
        dto.setDocType(entity.getDocType());
        dto.setFileUri(entity.getFileUri());
        dto.setVerificationStatus(entity.getVerificationStatus());
        return dto;
    }

    /**
     * Converts a single {@link ApplicationValidation} entity to a {@link ValidationResultDTO}.
     *
     * <p>The {@code passed} boolean field is derived by case-insensitively comparing
     * the stored {@code result} string to {@code "PASSED"}.</p>
     *
     * @param validation the {@link ApplicationValidation} entity to convert;
     *                   may be {@code null}
     * @return a {@link ValidationResultDTO} with rule name, raw result string,
     *         derived pass/fail flag, message, and validation timestamp, or
     *         {@code null} if {@code validation} is {@code null}
     */
    public ValidationResultDTO toValidationDTO(ApplicationValidation validation) {
        if (validation == null) return null;
        ValidationResultDTO dto = new ValidationResultDTO();
        dto.setRuleName(validation.getRuleName());
        dto.setResult(validation.getResult());
        dto.setPassed("PASSED".equalsIgnoreCase(validation.getResult()));
        dto.setMessage(validation.getMessage());
        dto.setValidatedAt(validation.getValidatedAt());
        return dto;
    }

    // --- Private Helpers for the ResponseDTO Builder ---

    private List<DocumentDTO> mapDocuments(List<Document> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(this::toDocumentDTO).collect(Collectors.toList());
    }

    private List<ValidationDTO> mapValidationsToDTO(List<ApplicationValidation> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(val -> {
                    ValidationDTO dto = new ValidationDTO();
                    dto.setRuleName(val.getRuleName());
                    dto.setResult(val.getResult());
                    dto.setMessage(val.getMessage());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}