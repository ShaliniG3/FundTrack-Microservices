package com.cts.fundtrack.application.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.ApplicationStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a grant funding application submitted by an applicant
 * in the FundTrack system.
 *
 * <p>An {@code Application} is the central aggregate of the Application Service.
 * It links an applicant (via {@code applicantId}, a reference to the Identity
 * Service) to a specific grant program (via {@code programId}, a reference to
 * the Program Service) and tracks the full lifecycle of the funding request
 * from initial submission through review, decision, and beyond.</p>
 *
 * <p>Associated collections are managed as owned one-to-many relationships:
 * <ul>
 *   <li>{@link Document} — supporting evidence files uploaded by the applicant</li>
 *   <li>{@link ApplicationValidation} — results of the automated SpEL-based
 *       eligibility rule evaluations run at submission and update time</li>
 * </ul>
 * Both collections are cascade-all with orphan removal, so child records are
 * fully managed through the parent entity.</p>
 *
 * <p>The {@code status} field progresses through values defined in
 * {@link ApplicationStatus}: {@code SUBMITTED} → {@code UNDER_REVIEW} →
 * {@code APPROVED} or {@code REJECTED}.</p>
 */
@Entity
@Table(name = "applications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    /** Auto-generated UUID primary key for this application record. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID applicationId;

    /**
     * UUID of the applicant who submitted this application.
     * References a user record in the Identity Service; not a foreign key
     * in this database schema.
     */
    @Column(nullable = false)
    private UUID applicantId;

    /**
     * UUID of the grant program this application targets.
     * References a program record in the Program Service; not a foreign key
     * in this database schema.
     */
    @Column(nullable = false)
    private UUID programId;

    /**
     * Free-form text payload containing the applicant's responses to the
     * program application form, stored as a comma-separated key=value string
     * (e.g., {@code "income=30000,age=25"}) that is evaluated by the SpEL
     * eligibility validation engine.
     */
    @Column(columnDefinition = "TEXT")
    private String applicationData;

    /** Current lifecycle status of this application. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ApplicationStatus status;

    /** Timestamp of when this application was first persisted. */
    private LocalDateTime createdAt;

    /** Timestamp of the most recent update to this application record. */
    private LocalDateTime updatedAt;

    /** Supporting documents uploaded by the applicant for this application. */
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    /** Automated eligibility rule evaluation results for this application. */
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApplicationValidation> validations = new ArrayList<>();

    /**
     * JPA lifecycle callback that initialises {@code createdAt} and
     * {@code updatedAt} to the current timestamp before the entity is first
     * persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback that refreshes {@code updatedAt} to the current
     * timestamp whenever the entity is updated in the database.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}