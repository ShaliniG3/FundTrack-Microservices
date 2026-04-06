package com.fundtrack.modules.application_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.fundtrack.modules.application_service.models.Application;
import com.fundtrack.modules.application_service.models.enums.ApplicationStatus;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /**
     * Finds applications by status for specific actor dashboards[cite: 102].
     * Example: Fetching all 'SUBMITTED' applications for Reviewers.
     */
    List<Application> findAllByStatus(ApplicationStatus status);

    boolean existsByApplicantIdAndProgramId(UUID applicantId, UUID programId);
    
}
