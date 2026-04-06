package com.fundtrack.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.fundtrack.modules.application_service.dto.EligibilityRuleDTO;
import com.fundtrack.modules.application_service.dto.ProgramRequirementsDTO;

@Component
public class ProgramServiceClientFallback implements ProgramServiceClient {

    @Override
    public List<EligibilityRuleDTO> getRulesByProgramId(UUID programId) {
        // Mocking rules so your evaluateRule() logic has something to check
        List<EligibilityRuleDTO> mockRules = new ArrayList<>();
        
        EligibilityRuleDTO rule1 = new EligibilityRuleDTO();
        rule1.setRuleExpression("Income < 50000");
        
        EligibilityRuleDTO rule2 = new EligibilityRuleDTO();
        rule2.setRuleExpression("Age > 18");
        
        mockRules.add(rule1);
        mockRules.add(rule2);
        
        return mockRules;
    }

    @Override
    public ProgramRequirementsDTO getRequirements(UUID programId) {
        // Mocking requirements for the dashboard view
        ProgramRequirementsDTO mockReq = new ProgramRequirementsDTO();
        mockReq.setProgramName("Mock Testing Program");
        return mockReq;
    }
}