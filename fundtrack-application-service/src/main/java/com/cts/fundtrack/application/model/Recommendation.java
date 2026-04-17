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

/**
 * JPA entity representing a reviewer's recommendation for a grant application
 * in the FundTrack system.
 *
 * <p>A {@code Recommendation} is created or updated alongside a {@link Review}
 * during the reviewer evaluation stage. It captures the reviewer's advisory
 * opinion — typically {@code "Recommended"} or {@code "Not Recommended"} — along
 * with a written justification. This information is surfaced to the approver via
 * the {@link com.cts.fundtrack.application.service.DecisionService} to inform the
 * final funding decision.</p>
 *
 * <p>Unlike a {@link Decision}, a recommendation is non-binding; the approver
 * may override it when issuing the final outcome. One recommendation record
 * exists per application; if a reviewer updates their recommendation, the
 * existing record is overwritten rather than creating a new one.</p>
 */
@Entity
@Table(name = "recommendations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    /** Auto-generated UUID primary key for this recommendation record. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID recommendationId;

    /**
     * UUID of the grant application this recommendation is associated with.
     * Logical reference to the {@code applications} table.
     */
    private UUID applicationId;

    /**
     * UUID of the user (with the {@code REVIEWER} role) who issued this
     * recommendation. Logical reference to the Identity Service user registry.
     */
    private UUID reviewerId;

    /**
     * The advisory outcome string, typically {@code "Recommended"} or
     * {@code "Not Recommended"}, as determined by the reviewer.
     */
    private String decision;

    /** The reviewer's written justification for the recommendation. */
    private String notes;

    /** The calendar date on which this recommendation was last saved. */
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