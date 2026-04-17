package com.cts.fundtrack.application.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing the result of a single automated eligibility rule
 * evaluation for a grant application in the FundTrack system.
 *
 * <p>When an {@link Application} is submitted or updated, the Application Service
 * fetches the SpEL-based eligibility rules defined for the target program from
 * the Program Service and evaluates each rule against the applicant's submitted
 * data. One {@code ApplicationValidation} record is persisted per rule, capturing
 * whether the applicant met that criterion.</p>
 *
 * <p>Each record is owned by its parent {@link Application} through a many-to-one
 * relationship, and the full set of validations is cascade-managed via the parent
 * entity's {@code validations} collection.</p>
 */
@Entity
@Table(name = "application_validations")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationValidation {

    /** Auto-generated UUID primary key for this validation result record. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID validationId;

    /**
     * The parent application to which this validation result belongs.
     * Loaded lazily to avoid unnecessary joins when only the validation
     * data itself is needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    /**
     * The SpEL expression string that was evaluated, used as a human-readable
     * rule identifier (e.g., {@code "income >= 10000"}).
     */
    private String ruleName;

    /**
     * The outcome of the rule evaluation: {@code "PASSED"} if the applicant's
     * data satisfied the expression, or {@code "FAILED"} otherwise.
     */
    private String result;

    /**
     * A human-readable explanation of the result — either a generic success
     * message or a description of the unmet criterion.
     */
    private String message;

    /** Timestamp at which this validation was persisted. */
    private LocalDateTime validatedAt;

    /**
     * JPA lifecycle callback that sets {@code validatedAt} to the current
     * timestamp immediately before this record is first persisted.
     */
    @PrePersist
    protected void onValidate() {
        validatedAt = LocalDateTime.now();
    }
}