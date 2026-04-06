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
@Table(name = "reviews")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id // ADD THIS ANNOTATION
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;
    private UUID applicationId;
    private UUID reviewerId;
    private Integer score;
    private String comments;
    private LocalDate date;
}
