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

@Entity
@Table(name = "application_validations")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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