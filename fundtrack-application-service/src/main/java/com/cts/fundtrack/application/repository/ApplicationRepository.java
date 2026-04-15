package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.common.models.enums.ApplicationStatus;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    
    List<Application> findAllByStatus(ApplicationStatus status);

    boolean existsByApplicantIdAndProgramId(UUID applicantId, UUID programId);
}