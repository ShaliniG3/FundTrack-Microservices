package com.fundtrack.modules.application_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.fundtrack.modules.application_service.models.ApplicationValidation;


import jakarta.transaction.Transactional;

@Repository
public interface ApplicationValidationRepository extends JpaRepository<ApplicationValidation,UUID> {

    @Transactional
    void deleteByApplication_ApplicationId(UUID applicationId);

    List<ApplicationValidation> findByApplication_ApplicationId(UUID applicationId);
    
}
