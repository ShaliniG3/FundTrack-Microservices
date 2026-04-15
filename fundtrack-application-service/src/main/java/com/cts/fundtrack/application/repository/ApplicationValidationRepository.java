package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.model.ApplicationValidation;

@Repository
public interface ApplicationValidationRepository extends JpaRepository<ApplicationValidation, UUID> {

    @Transactional
    void deleteByApplication_ApplicationId(UUID applicationId);

    List<ApplicationValidation> findByApplication_ApplicationId(UUID applicationId);
}