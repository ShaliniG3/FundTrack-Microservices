package com.cts.fundtrack.program.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.program.models.EligibilityRule;

@Repository
public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, UUID> {
}