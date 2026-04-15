package com.cts.fundtrack.program.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequestDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.models.enums.ProgramStatus;

public interface ProgramService {
    ProgramResponseDTO createProgram(ProgramRequestDTO dto);
    List<ProgramResponseDTO> getAllPrograms();
    ProgramResponseDTO getProgramById(UUID programId);
    ProgramResponseDTO updateProgram(UUID programId, ProgramRequestDTO dto);
    void archiveProgram(UUID programId);
    void deleteProgram(UUID programId);
    List<ProgramResponseDTO> searchPrograms(String keyWord);
    ProgramResponseDTO updateProgramStatus(UUID programId, ProgramStatus status);
    
 // Add these to your ProgramService interface
    List<EligibilityRuleDTO> getRulesByProgramId(UUID programId);
    ProgramRequirementsDTO getRequirements(UUID programId);
    
}