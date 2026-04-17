package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.application.model.Document;

/**
 * Spring Data JPA repository for {@link Document} entities.
 *
 * <p>Provides persistence operations for supporting documents uploaded by applicants
 * as part of their grant applications. Documents are cascade-deleted with their
 * parent {@link com.cts.fundtrack.application.model.Application}, but this repository
 * also supports direct bulk retrieval and counting by application ID for use in
 * the decision and validation workflows.</p>
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Retrieves all documents associated with a specific grant application.
     *
     * <p>Used by the decision workflow to update verification statuses in bulk
     * when a final funding decision is processed, amended, or revoked.</p>
     *
     * @param applicationId the UUID of the application whose documents are requested
     * @return a list of {@link Document} entities for the application; empty if no
     *         documents have been uploaded
     */
    List<Document> findByApplication_ApplicationId(UUID applicationId);

    /**
     * Returns the total number of documents uploaded for a specific application.
     *
     * <p>Useful for validation checks and dashboard summary statistics without
     * loading the full document collection into memory.</p>
     *
     * @param applicationId the UUID of the application to count documents for
     * @return the number of {@link Document} records associated with the application
     */
    long countByApplication_ApplicationId(UUID applicationId);
}