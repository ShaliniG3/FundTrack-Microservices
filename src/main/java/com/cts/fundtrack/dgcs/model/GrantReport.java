package com.cts.fundtrack.dgcs.model;

import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence entity representing a grant progress report.
 * <p>
 * This model serves as the primary record for post-award accountability,
 * capturing milestone narratives (scope), performance data (metrics),
 * and digital evidence (proofPath). It is linked to the parent grant
 * application via a logical UUID reference.
 * </p>
 */

@Entity
@Table(name = "grant_reports")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrantReport {

    /**
     * Primary key. Uses UUID version 4 to prevent ID enumeration.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id", updatable = false, nullable = false)
    private UUID grantReportId;

    /**
     * Logical reference to the associated Grant Application.
     */
    @NotNull(message = "Application ID is mandatory")
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    /**
     * High-level summary of the report's objectives or boundaries.
     */
    @NotBlank(message = "Scope summary is required")
    @Column(nullable = false)
    private String scope;

    /**
     * Detailed performance metrics stored as unstructured text or JSON.
     */
    @Column(columnDefinition = "TEXT")
    private String metrics;

    /**
     * The audit timestamp indicating when the report was officially persisted.
     */
    @Column(nullable = false,updatable = false)
    private Instant submittedDate;

    /**
     * The current processing state within the report lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GrantReportStatus status;

    /**
     * Storage reference or path to the uploaded PDF/document evidence.
     */
    @Column(name = "proof_path")
    private String proofPath;

    /**
     * Lifecycle hook to ensure the submission timestamp and default status
     * are automatically captured upon initial record creation. [cite: 6, 108]
     */
    @PrePersist
    @SuppressWarnings("unused")
    protected void onCreate() {
        if (this.submittedDate == null) {
            this.submittedDate = Instant.now();
        }
        if (this.status == null) {
            this.status = GrantReportStatus.SUBMITTED;
        }

    }
}