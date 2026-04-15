package com.cts.fundtrack.program.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "required_documents")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RequiredDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnore // Prevents infinite loop during JSON serialization
    private Program program; 

    @Column(nullable = false)
    private String name; 

    @Column(nullable = false)
    private Boolean mandatory;
}