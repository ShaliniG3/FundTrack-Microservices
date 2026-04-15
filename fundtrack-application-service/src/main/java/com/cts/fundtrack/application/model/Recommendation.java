package com.cts.fundtrack.application.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recommendations")
@Getter 
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID recommendationId;
    
    private UUID applicationId;
    
    private UUID reviewerId;
    
    /**
     * e.g., "Recommended" or "Not Recommended"
     */
    private String decision; 
    
    /**
     * The justification for the recommendation
     */
    private String notes;
    
    private LocalDate date;

    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = LocalDate.now();
        }
    }
}