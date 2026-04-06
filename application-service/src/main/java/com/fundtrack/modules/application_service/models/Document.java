package com.fundtrack.modules.application_service.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fundtrack.modules.application_service.models.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID documentId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    private String docType;
    private String fileUri;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
}