package com.fundtrack.modules.decision_service.service;

import com.fundtrack.exceptions.ApplicationNotFoundException;
import com.fundtrack.exceptions.ApplicationNotReadyForDecisionException;
import com.fundtrack.exceptions.InvalidDecisionTypeException;
import com.fundtrack.modules.application_service.dto.ApplicationResponseDTO;
import com.fundtrack.modules.application_service.mappers.ApplicationMapper;
import com.fundtrack.modules.application_service.models.Application;
import com.fundtrack.modules.application_service.models.Document;
import com.fundtrack.modules.application_service.models.enums.ApplicationStatus;
import com.fundtrack.modules.application_service.models.enums.VerificationStatus;
import com.fundtrack.modules.application_service.repository.ApplicationRepository;
import com.fundtrack.modules.application_service.repository.DocumentRepository;
import com.fundtrack.modules.decision_service.dto.ApplicationDecisionDetailsDTO;
import com.fundtrack.modules.decision_service.dto.ApproverDashBoardDTO;
import com.fundtrack.modules.decision_service.dto.DecisionRequestDTO;
import com.fundtrack.modules.decision_service.models.Decision;
import com.fundtrack.modules.decision_service.repository.DecisionRepository;
import com.fundtrack.modules.review_service.models.Recommendation;
import com.fundtrack.modules.review_service.models.Review;
import com.fundtrack.modules.review_service.repositories.RecommendationRepository;
import com.fundtrack.modules.review_service.repositories.ReviewRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DecisionServiceImpl implements DecisionService {

    private final DecisionRepository decisionRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationMapper applicationMapper;
    private final ReviewRepository reviewRepository;
    private final RecommendationRepository recommendationRepository;

   public List<ApplicationResponseDTO> getApplicationsUnderReview() {
    return applicationRepository.findAll().stream()
            // Assuming decision makers only look at applications currently UNDER_REVIEW
            .filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW)
            .map(applicationMapper::toResponseDTO) // <--- THIS IS THE MAGIC LINE
            .collect(Collectors.toList());
}

    @Override
    public ApplicationDecisionDetailsDTO getApplicationById(UUID applicationId) {
       Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        
        // Convert to DTO to prevent infinite JSON loops!
        ApplicationResponseDTO appDto = applicationMapper.toResponseDTO(app);

        // Fetch Review and Recommendation (if they exist)
        Review review = reviewRepository.findByApplicationId(applicationId).orElse(null);
        Recommendation rec = recommendationRepository.findByApplicationId(applicationId).orElse(null);

        return new ApplicationDecisionDetailsDTO(appDto, review, rec);
    }

    public ApproverDashBoardDTO getDecisionsByApprover(UUID approverId) {
        List<Decision> decisions = decisionRepository.findByApproverId(approverId);
        return new ApproverDashBoardDTO(decisions.size(), decisions);
    }

    @Override
    @Transactional
    public void processDecision(DecisionRequestDTO dto) {
        Application app = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(dto.getApplicationId()));

        // Business Rule: Only finalise if status is UNDER_REVIEW
        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationNotReadyForDecisionException(dto.getApplicationId(), app.getStatus().name());
        }

        // 1. Validate Decision String before conversion
        ApplicationStatus finalStatus;
        try {
            finalStatus = ApplicationStatus.valueOf(dto.getDecision().toUpperCase());
            if (finalStatus != ApplicationStatus.APPROVED && finalStatus != ApplicationStatus.REJECTED) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidDecisionTypeException(dto.getDecision());
        }

        // 2. Manual Mapping using Builder
        Decision decision = Decision.builder()
                .applicationId(dto.getApplicationId())
                .approverId(dto.getApproverId())
                .decision(dto.getDecision())
                .notes(dto.getNotes())
                .date(LocalDate.now())
                .build();
        
        decisionRepository.save(decision);

        // 3. Transition Application to Terminal Status
        app.setStatus(finalStatus);
        applicationRepository.save(app);

        // 4. Update Documents upon Rejection
        if (ApplicationStatus.REJECTED.equals(finalStatus)) {
            List<Document> documents = documentRepository.findByApplication_ApplicationId(dto.getApplicationId());
            for (Document doc : documents) {
                doc.setVerificationStatus(VerificationStatus.DOCUMENT_REJECTED);
                documentRepository.save(doc);
            }
        }
        else{
            List<Document> documents = documentRepository.findByApplication_ApplicationId(dto.getApplicationId());
            for (Document doc : documents) {
                doc.setVerificationStatus(VerificationStatus.DOCUMENT_APPROVED);
                documentRepository.save(doc);
            }

        }
    }

    @Override
    @Transactional
    public void deleteDecisionByApplicationId(UUID applicationId) {
        Decision decision = decisionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Decision not found for Application ID: " + applicationId));

        // 1. Delete the decision
        decisionRepository.delete(decision);

        // 2. Revert Application Status
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(app);

        // 3. Revert Document Statuses
        List<Document> documents = documentRepository.findByApplication_ApplicationId(applicationId);
        for (Document doc : documents) {
            doc.setVerificationStatus(VerificationStatus.SUBMITTED); // Reset back to submitted
            documentRepository.save(doc);
        }
    }
}