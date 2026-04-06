package com.fundtrack.modules.review_service.models;

import java.time.LocalDate;
import java.util.UUID;


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

@Entity
@Table(name = "recommendations")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation  {
    @Id // ADD THIS ANNOTATION
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID recommendationId;
    private UUID applicationId;
    private UUID reviewerId;
    private String decision; // e.g., "Recommended" or "Not Recommended"
    private String notes;
    private LocalDate date;
}
