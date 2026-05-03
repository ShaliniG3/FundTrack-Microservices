package com.cts.fundtrack.program.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.program.models.EligibilityRule;

/**
 * Spring Data JPA repository for {@link EligibilityRule} entities.
 *
 * <p>Extends {@link JpaRepository} to inherit standard CRUD operations (save, find,
 * delete, etc.) for eligibility rules. In the current design, eligibility rules are
 * managed exclusively through their parent {@link com.cts.fundtrack.program.models.Program}
 * entity via JPA cascade ({@code CascadeType.ALL} and {@code orphanRemoval = true}),
 * so direct rule-level persistence calls through this repository are rarely required.</p>
 *
 * <p>This repository is declared to satisfy Spring's dependency injection and to allow
 * future rule-specific queries (e.g., searching rules by expression pattern) to be added
 * without architectural changes.</p>
 */
@Repository
public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, UUID> {
}
