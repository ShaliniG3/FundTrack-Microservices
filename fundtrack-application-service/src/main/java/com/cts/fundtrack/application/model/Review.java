package com.cts.fundtrack.application.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a technical reviewer's scored evaluation of a grant
 * application in the FundTrack system.
 *
 * <p>A {@code Review} is created when a user with the {@code REVIEWER} role
 * processes a {@code SUBMITTED} application. The reviewer assigns a numeric
 * score (0–100), adds qualitative comments, and optionally attaches a
 * {@link Recommendation}. Saving a review transitions the parent application's
 * status from {@code SUBMITTED} to {@code UNDER_REVIEW}.</p>
 *
 * <p>One review record exists per application. If a reviewer subsequently calls
 * the patch endpoint, the existing record is updated in-place rather than a new
 * one being created.</p>
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    /** Auto-generated UUID primary key for this review record. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

    /**
     * UUID of the grant application being reviewed.
     * Logical reference to the {@code applications} table.
     */
    private UUID applicationId;

    /**
     * UUID of the user (with the {@code REVIEWER} role) who conducted this review.
     * Logical reference to the Identity Service user registry.
     */
    private UUID reviewerId;

    /**
     * Numeric quality score assigned by the reviewer, on a scale of 0 to 100.
     * Values outside this range are rejected by the service layer before
     * a record is persisted.
     */
    private Integer score;

    /** Free-text qualitative feedback and observations from the reviewer. */
    private String comments;

    /** The calendar date on which this review was last saved. */
    private LocalDate date;

    /**
     * JPA lifecycle callback that sets {@code date} to today's date before
     * the record is first persisted, if the date has not already been set
     * explicitly.
     */
    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = LocalDate.now();
        }
    }
}