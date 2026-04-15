package com.cts.fundtrack.program.models;

import com.cts.fundtrack.common.models.enums.ProgramStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "programs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "program_id", updatable = false, nullable = false)
    private UUID programId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProgramStatus status;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EligibilityRule> eligibilityRules = new ArrayList<>();

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequiredDocument> requiredDocuments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ProgramStatus.DRAFT;
        }
    }

    // Business Logic: Helps decide if applications should be allowed
    public boolean isClosed() {
        if (this.status == ProgramStatus.CLOSED || this.status == ProgramStatus.ARCHIVED) {
            return true;
        }
        return endDate != null && LocalDate.now().isAfter(endDate);
    }
}