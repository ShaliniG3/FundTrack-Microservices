package com.cts.fundtrack.application.service;

import com.cts.fundtrack.common.dto.*;
import java.util.List;
import java.util.UUID;

public interface DecisionService {
    List<ApplicationResponseDTO> getApplicationsUnderReview();
    ApplicationDecisionDetailsDTO getApplicationById(UUID applicationId);
    void processDecision(DecisionRequestDTO dto);
    void deleteDecisionByApplicationId(UUID applicationId);
    ApproverDashBoardDTO getDecisionsByApprover(UUID approverId);
}