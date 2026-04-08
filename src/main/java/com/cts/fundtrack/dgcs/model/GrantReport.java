package com.cts.fundtrack.dgcs.model;

import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grant_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrantReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id", updatable = false, nullable = false)
    private UUID reportId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(length = 255)
    private String scope;

    @Column(columnDefinition = "TEXT")
    private String metrics;

    @Column(nullable = false)
    private Instant submittedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GrantReportStatus status;

    @Column(name = "proof_path")
    private String proofPath;

    @PrePersist
    protected void onCreate() {
        if (this.submittedDate == null) {
            this.submittedDate = Instant.now();
        }
    }
}