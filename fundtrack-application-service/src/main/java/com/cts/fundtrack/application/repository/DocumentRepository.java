package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.application.model.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document,UUID> {

    List<Document> findByApplication_ApplicationId(UUID applicationId);

    long countByApplication_ApplicationId(UUID applicationId);
    
}