package com.fundtrack.modules.application_service.mappers;

import org.springframework.stereotype.Component;
import com.fundtrack.modules.application_service.dto.*;
import com.fundtrack.modules.application_service.models.*;
import java.util.stream.Collectors;

@Component
public class ApplicationMapper {

    /**
     * Maps the incoming Request DTO to the Application Entity.
     * Note: applicantId is usually set in the Service layer from the session.
     */
    public Application toEntity(ApplicationRequestDTO dto) {
        if (dto == null) return null;
        
        Application entity = new Application();
        entity.setProgramId(dto.getProgramId());
        entity.setApplicationData(dto.getApplicationData());
        // applicantId is intentionally left to be set by the Service 
        // to ensure it comes from a secure source (Header/Session).
        
        return entity;
    }

    /**
     * Maps the Entity to the Response DTO shown in your Postman results.
     * This ensures the frontend gets a consolidated view of docs and validations.
     */
    public ApplicationResponseDTO toResponseDTO(Application entity) {
        if (entity == null) return null;

        ApplicationResponseDTO dto = new ApplicationResponseDTO();
        dto.setApplicationId(entity.getApplicationId());
        dto.setApplicantId(entity.getApplicantId());
        dto.setProgramId(entity.getProgramId());
        
        if (entity.getStatus() != null) {
            dto.setStatus(entity.getStatus().name());
        }
        
        dto.setSubmittedDate(entity.getCreatedAt());
        
        return dto;
    }

    public ApplicantDetailsDTO toFullDetailsDTO(Application entity) {
        if(entity == null) return null;

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

    public ValidationResultDTO toValidationDTO(ApplicationValidation validation) {
        if(validation == null) return null;

        ValidationResultDTO dto = new ValidationResultDTO();
        dto.setRuleName(validation.getRuleName());
        dto.setResult(validation.getResult());
        dto.setMessage(validation.getMessage());
        // Using getCreatedAt() or getValidatedAt() depending on your model field name
        dto.setValidatedAt(validation.getValidatedAt()); 
        
        return dto;
    }

    public DocumentDTO toDocumentDTO(Document entity){
        if(entity == null) return null;
        
        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentId(entity.getDocumentId());
        dto.setDocType(entity.getDocType());
        dto.setFileUri(entity.getFileUri());
        dto.setVerificationStatus(entity.getVerificationStatus());
        
        return dto;
    }
}