package com.fundtrack.modules.application_service.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_validations")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID validationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    private String ruleName;
    private String result; // e.g., PASSED, FAILED
    private String message;
    private LocalDateTime validatedAt;

    @PrePersist
    protected void onValidate() {
        validatedAt = LocalDateTime.now();
    }
}