package com.cts.fundtrack.disbursement.models;

import java.time.Instant;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.ComplianceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence entity representing a formal compliance verification record.
 * <p>
 * This model captures the results of regulatory checks performed on grant reports
 * or applications. It serves as an audit trail for fiduciary oversight, linking
 * the compliance outcome to the specific officer responsible for the validation.
 * </p>
 */

@Entity
@Table(name = "compliance_checks")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceCheck {

    /**
     * Internal unique identifier for the compliance audit record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "check_id", updatable = false, nullable = false)
    private UUID checkId;

    /**
     * Reference to the Grant Report that triggered this compliance check.
     */
    @NotNull(message = "Grant Report ID is mandatory")
    @Column(name = "grant_report_id", nullable = false)
    private UUID grantReportId;

    /**
     * Reference to the parent Application being audited.
     */
    @NotNull(message = "Application ID is mandatory")
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    /**
     * Unique identifier of the Compliance Officer who performed the manual
     * or automated verification.
     */
    @NotNull(message = "Compliance Officer ID is mandatory")
    @Column(name = "compliance_officer_id", nullable = false)
    private UUID complianceOfficerId;

    /**
     * The category of verification performed (e.g., FINANCIAL_AUDIT,
     * MILESTONE_VERIFICATION, DOCUMENTATION_REVIEW).
     */
    @NotBlank(message = "Verification type is required")
    @Column(name = "verification_type", nullable = false, length = 50)
    private String type;

    /**
     * The finalized outcome of the compliance verification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 50,nullable = false)
    private ComplianceStatus result;

    /**
     * Timestamp indicating when the verification was finalized.
     */
    @Column(nullable = false, updatable = false)
    private Instant date;

    /**
     * Detailed observations, justification, or remedial actions required
     * based on the check results.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Automatically captures the verification timestamp if not manually provided.
     */
    @PrePersist
    @SuppressWarnings("unused")
    protected void onCreate() {
        if (this.date == null) {
            this.date = Instant.now();
        }
    }
}