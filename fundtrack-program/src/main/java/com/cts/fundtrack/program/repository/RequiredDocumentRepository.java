package com.cts.fundtrack.program.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.program.models.RequiredDocument;

/**
 * Spring Data JPA repository for {@link RequiredDocument} entities.
 *
 * <p>Extends {@link JpaRepository} to inherit standard CRUD operations for required
 * document records. Like {@link EligibilityRuleRepository}, required documents are
 * managed through their parent {@link com.cts.fundtrack.program.models.Program} entity
 * via JPA cascade ({@code CascadeType.ALL} and {@code orphanRemoval = true}), so
 * direct document-level persistence through this repository is rarely needed in practice.</p>
 *
 * <p>This repository is declared to satisfy Spring's dependency injection and to serve
 * as an extension point for future document-specific queries (e.g., finding all mandatory
 * documents across all active programs).</p>
 */
@Repository
public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, UUID> {
}
