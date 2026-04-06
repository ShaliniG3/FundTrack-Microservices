package com.fundtrack.modules.decision_service.service;

import com.fundtrack.modules.application_service.dto.ApplicationResponseDTO;
import com.fundtrack.modules.decision_service.dto.ApplicationDecisionDetailsDTO;
import com.fundtrack.modules.decision_service.dto.ApproverDashBoardDTO;
import com.fundtrack.modules.decision_service.dto.DecisionRequestDTO;

import java.util.List;
import java.util.UUID;

public interface DecisionService {
    List<ApplicationResponseDTO> getApplicationsUnderReview();
    ApplicationDecisionDetailsDTO getApplicationById(UUID applicationId);
    void processDecision(DecisionRequestDTO dto);
    void deleteDecisionByApplicationId(UUID applicationId);
    ApproverDashBoardDTO getDecisionsByApprover(UUID approverId);
}