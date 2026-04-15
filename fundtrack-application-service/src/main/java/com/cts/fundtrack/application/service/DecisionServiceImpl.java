package com.cts.fundtrack.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.mapper.ApplicationMapper;
import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.application.model.Decision;
import com.cts.fundtrack.application.model.Document;
import com.cts.fundtrack.application.repository.ApplicationRepository;
import com.cts.fundtrack.application.repository.DecisionRepository;
import com.cts.fundtrack.application.repository.DocumentRepository;
import com.cts.fundtrack.application.repository.RecommendationRepository;
import com.cts.fundtrack.application.repository.ReviewRepository;
import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.dto.ApplicationDecisionDetailsDTO;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ApproverDashBoardDTO;
import com.cts.fundtrack.common.dto.DecisionDTO;
import com.cts.fundtrack.common.dto.DecisionRequestDTO;
import com.cts.fundtrack.common.dto.RecommendationDTO;
import com.cts.fundtrack.common.dto.ReviewDTO;
import com.cts.fundtrack.common.exceptions.ApplicationNotFoundException;
import com.cts.fundtrack.common.exceptions.ApplicationNotReadyForDecisionException;
import com.cts.fundtrack.common.exceptions.InvalidDecisionTypeException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.ApplicationStatus;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.VerificationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DecisionServiceImpl implements DecisionService {

    private final DecisionRepository decisionRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationMapper applicationMapper;
    private final ReviewRepository reviewRepository;
    private final RecommendationRepository recommendationRepository;

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public List<ApplicationResponseDTO> getApplicationsUnderReview() {
        return applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW)
                .map(applicationMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override  
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public ApplicationDecisionDetailsDTO getApplicationById(UUID applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        
        ApplicationResponseDTO appDto = applicationMapper.toResponseDTO(app);

        ReviewDTO reviewDto = reviewRepository.findByApplicationId(applicationId)
                .map(rev -> ReviewDTO.builder()
                        .reviewId(rev.getReviewId())
                        .applicationId(rev.getApplicationId())
                        .score(rev.getScore())
                        .comments(rev.getComments())
                        .build())
                .orElse(null);

        RecommendationDTO recDto = recommendationRepository.findByApplicationId(applicationId)
                .map(rec -> RecommendationDTO.builder()
                        .recommendationId(rec.getRecommendationId())
                        .recommendationStatus(rec.getDecision())
                        .justification(rec.getNotes())
                        .build())
                .orElse(null);

        return ApplicationDecisionDetailsDTO.builder()
                .applicationDetails(appDto)
                .review(reviewDto)
                .recommendation(recDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.DECISION)
    public ApproverDashBoardDTO getDecisionsByApprover(UUID approverId) {
        List<Decision> decisions = decisionRepository.findByApproverId(approverId);
        
        List<DecisionDTO> decisionDTOs = decisions.stream()
                .map(d -> DecisionDTO.builder()
                        .decisionId(d.getDecisionId())
                        .applicationId(d.getApplicationId())
                        .approverId(d.getApproverId())
                        .decision(d.getDecision())
                        .notes(d.getNotes())
                        .date(d.getDate())
                        .build())
                .collect(Collectors.toList());

        return ApproverDashBoardDTO.builder()
                .count(decisionDTOs.size())
                .decisions(decisionDTOs)
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.STATUS_CHANGE, entityName = EntityType.APPLICATION)
    public void processDecision(DecisionRequestDTO dto) {
        Application app = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(dto.getApplicationId()));

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationNotReadyForDecisionException(dto.getApplicationId(), app.getStatus().name());
        }

        ApplicationStatus finalStatus;
        try {
            finalStatus = ApplicationStatus.valueOf(dto.getDecision().toUpperCase());
            if (finalStatus != ApplicationStatus.APPROVED && finalStatus != ApplicationStatus.REJECTED) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidDecisionTypeException(dto.getDecision());
        }

        Decision decision = Decision.builder()
                .applicationId(dto.getApplicationId())
                .approverId(dto.getApproverId())
                .decision(dto.getDecision())
                .notes(dto.getNotes())
                .date(LocalDate.now())
                .build();
        
        decisionRepository.save(decision);

        app.setStatus(finalStatus);
        applicationRepository.save(app);

        VerificationStatus docStatus = ApplicationStatus.REJECTED.equals(finalStatus) 
                ? VerificationStatus.DOCUMENT_REJECTED 
                : VerificationStatus.DOCUMENT_APPROVED;

        List<Document> documents = documentRepository.findByApplication_ApplicationId(dto.getApplicationId());
        for (Document doc : documents) {
            doc.setVerificationStatus(docStatus);
            documentRepository.save(doc);
        }
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.DECISION)
    public void deleteDecisionByApplicationId(UUID applicationId) {
        Decision decision = decisionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Decision not found"));

        decisionRepository.delete(decision);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(app);

        List<Document> documents = documentRepository.findByApplication_ApplicationId(applicationId);
        for (Document doc : documents) {
            doc.setVerificationStatus(VerificationStatus.SUBMITTED);
            documentRepository.save(doc);
        }
    }
}