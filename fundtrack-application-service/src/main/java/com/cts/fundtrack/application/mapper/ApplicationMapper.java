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

@Component
public class ApplicationMapper {

    /**
     * Maps Request DTO to Entity.
     */
    public Application toEntity(ApplicationRequestDTO dto) {
        if (dto == null) return null;
        Application entity = new Application();
        entity.setProgramId(dto.getProgramId());
        // applicantId is set in the Service layer
        return entity;
    }

    /**
     * Maps Entity to the Standard Response DTO (used for /submit).
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
     * Maps Entity to Full Details DTO (used for /summary).
     * This fixes the "toFullDetailsDTO is undefined" error.
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
     * Maps a single Document entity to DTO.
     * Required for stream mapping in the Service.
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
     * Maps a single ApplicationValidation entity to ValidationResultDTO.
     * This fixes the "toValidationDTO is undefined" error.
     */
    public ValidationResultDTO toValidationDTO(ApplicationValidation validation) {
        if (validation == null) return null;
        ValidationResultDTO dto = new ValidationResultDTO();
        dto.setRuleName(validation.getRuleName());
        dto.setResult(validation.getResult());
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