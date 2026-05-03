package com.cts.fundtrack.program.models;

import com.cts.fundtrack.common.models.enums.ProgramStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing a grant funding program within the FundTrack system.
 *
 * <p>A {@code Program} is the central aggregate of the Program Microservice. It defines
 * the terms, budget, and eligibility criteria for a funding opportunity that applicants
 * can discover and apply to. Each program owns two child collections:</p>
 * <ul>
 *   <li>{@link EligibilityRule} — business rules that determine whether an applicant
 *       qualifies for this program.</li>
 *   <li>{@link RequiredDocument} — documents that applicants must submit as part of
 *       their application.</li>
 * </ul>
 *
 * <p>Both child collections are managed with {@code CascadeType.ALL} and
 * {@code orphanRemoval = true}, meaning their full lifecycle (insert, update, delete)
 * is controlled through the parent {@code Program} entity.</p>
 *
 * <p>The lifecycle of a program is tracked via the {@link ProgramStatus} enum.
 * Programs are created in {@code DRAFT} status (enforced by the {@link #onCreate()}
 * pre-persist hook) and may transition through {@code ACTIVE}, {@code CLOSED},
 * and {@code ARCHIVED} states.</p>
 *
 * <p>Maps to the {@code programs} database table.</p>
 */
@Entity
@Table(name = "programs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Program {

    /**
     * Unique identifier for this program, generated as a UUID by the database.
     * This field is immutable after initial creation ({@code updatable = false}).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "program_id", updatable = false, nullable = false)
    private UUID programId;

    /**
     * The human-readable name of the grant funding program.
     * Maximum length is 200 characters; must not be null.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * A detailed description of the program's purpose, scope, and objectives.
     * Stored as a TEXT column to accommodate long-form content.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The date on which the program officially opens for applications.
     * May be {@code null} if no formal start date has been set.
     */
    private LocalDate startDate;

    /**
     * The date on which the program closes and stops accepting applications.
     * Used by {@link com.cts.fundtrack.program.service.ProgramScheduler} to
     * automatically transition the program to {@code CLOSED} status at midnight.
     * May be {@code null} if the program has no fixed end date.
     */
    private LocalDate endDate;

    /**
     * The total monetary budget allocated to this grant funding program.
     * May be {@code null} if no budget ceiling has been defined.
     */
    private Double budget;

    /**
     * The current lifecycle status of the program.
     * Stored as its string name (e.g., {@code "DRAFT"}, {@code "ACTIVE"}).
     * Defaults to {@link ProgramStatus#DRAFT} on first persist via {@link #onCreate()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProgramStatus status;

    /**
     * The set of eligibility rules that applicants must satisfy to qualify for this program.
     * Managed entirely by this parent entity via cascade and orphan removal.
     * Initialized to an empty list by default to avoid null-checks throughout the service layer.
     */
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EligibilityRule> eligibilityRules = new ArrayList<>();

    /**
     * The set of documents that applicants are required to submit with their application.
     * Managed entirely by this parent entity via cascade and orphan removal.
     * Initialized to an empty list by default to avoid null-checks throughout the service layer.
     */
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequiredDocument> requiredDocuments = new ArrayList<>();

    /**
     * JPA lifecycle callback invoked before the entity is first persisted.
     *
     * <p>Ensures that every new program starts in {@link ProgramStatus#DRAFT} status
     * if the service layer has not explicitly set a different value. This acts as a
     * safety net to prevent programs from accidentally entering production in an
     * undefined state.</p>
     */
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ProgramStatus.DRAFT;
        }
    }

    /**
     * Determines whether this program is effectively closed to new applications.
     *
     * <p>A program is considered closed if its status is explicitly {@code CLOSED} or
     * {@code ARCHIVED}, OR if its {@code endDate} has already passed (regardless of the
     * stored status). This method provides a business-level check that accounts for
     * programs that may not yet have been processed by the nightly
     * {@link com.cts.fundtrack.program.service.ProgramScheduler}.</p>
     *
     * @return {@code true} if the program is in {@code CLOSED} or {@code ARCHIVED} status,
     *         or if its {@code endDate} is in the past; {@code false} otherwise.
     */
    // Business Logic: Helps decide if applications should be allowed
    public boolean isClosed() {
        if (this.status == ProgramStatus.CLOSED || this.status == ProgramStatus.ARCHIVED) {
            return true;
        }
        return endDate != null && LocalDate.now().isAfter(endDate);
    }
}
