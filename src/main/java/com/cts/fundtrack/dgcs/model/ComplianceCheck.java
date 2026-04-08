package com.cts.fundtrack.dgcs.model;

import com.cts.fundtrack.dgcs.model.enums.ComplianceStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "compliance_checks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "check_id", updatable = false, nullable = false)
    private UUID checkId;

    @Column(name = "grant_report_id", nullable = false)
    private UUID grantReportId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "compliance_officer_id", nullable = false)
    private UUID complianceOfficerId;

    @Column(nullable = false, length = 50)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceStatus result;

    @Column(nullable = false)
    private Instant date;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = Instant.now();
        }
    }
}