package com.cts.fundtrack.program.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * JPA entity representing a single eligibility rule attached to a grant funding program.
 *
 * <p>Eligibility rules define the criteria that an applicant must satisfy in order to
 * qualify for a given {@link Program}. Each rule consists of a human-readable description
 * and a machine-evaluable expression that can be assessed against an applicant's profile
 * by the Application Service.</p>
 *
 * <p>Rules are owned by their parent {@link Program} via a many-to-one relationship.
 * The {@code @JsonIgnore} annotation on the {@code program} field prevents infinite
 * recursion during JSON serialization when a rule is serialized as part of a program
 * response.</p>
 *
 * <p>Maps to the {@code eligibility_rules} database table.</p>
 */
@Entity
@Table(name = "eligibility_rules")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EligibilityRule {

    /**
     * Unique identifier for this eligibility rule, generated as a UUID by the database.
     * This field is immutable after initial creation ({@code updatable = false}).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id", updatable = false, nullable = false)
    private UUID ruleId;

    /**
     * The parent grant funding program that this eligibility rule belongs to.
     *
     * <p>Loaded lazily to avoid unnecessary joins when only the rule's own fields are needed.
     * Annotated with {@code @JsonIgnore} to break the bidirectional serialization cycle
     * between {@link Program} and its rules collection.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnore // Prevents infinite loop during JSON serialization
    private Program program;

    /**
     * A plain-language description of what this rule checks.
     * For example: "Applicant must be a registered non-profit organization."
     * Must not be null.
     */
    @Column(nullable = false)
    private String ruleDescription;

    /**
     * A machine-evaluable expression encoding the rule logic.
     * For example: {@code "applicant.organizationType == 'NON_PROFIT'"}.
     * Evaluated by the Application Service to determine applicant eligibility.
     * Must not be null.
     */
    @Column(nullable = false)
    private String ruleExpression;
}
