package com.fundtrack.modules.decision_service.models;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID decisionId;

    private UUID applicationId; // Link to the specific grant application [cite: 60]
    
    private UUID approverId; // ID of the user with 'Approver' role [cite: 9, 60]
    
    private String decision; // Final status: APPROVED or REJECTED [cite: 60]
    
    private String notes; // Justification for the final decision [cite: 60]
    
    private LocalDate date; // Date the decision was finalized [cite: 60]
}