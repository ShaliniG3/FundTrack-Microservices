package com.cts.fundtrack.application.model;


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

/**
 * JPA entity representing a final funding decision issued by an approver for a
 * grant application in the FundTrack system.
 *
 * <p>A {@code Decision} record is created once an approver reviews the scorer's
 * evaluation and issues a binding {@code APPROVED} or {@code REJECTED} outcome.
 * It is the terminal record in the application lifecycle and triggers both a
 * status change on the parent {@link Application} and document verification
 * status updates on all associated {@link Document} records.</p>
 *
 * <p>The {@code applicationId} and {@code approverId} are logical foreign keys to
 * the Application Service's own {@code applications} table and the Identity
 * Service's user registry respectively; they are not enforced as database-level
 * constraints.</p>
 */
@Entity
@Table(name = "decisions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decision {

    /** Auto-generated UUID primary key for this decision record. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID decisionId;

    /**
     * UUID of the grant application to which this decision applies.
     * Logical reference to the {@code applications} table.
     */
    private UUID applicationId;

    /**
     * UUID of the user (with the {@code APPROVER} role) who issued this decision.
     * Logical reference to the Identity Service user registry.
     */
    private UUID approverId;

    /**
     * The final funding outcome: {@code "APPROVED"} or {@code "REJECTED"}.
     * Must correspond to a valid {@link com.cts.fundtrack.common.models.enums.ApplicationStatus}
     * terminal value.
     */
    private String decision;

    /** Optional free-text justification or rationale provided by the approver. */
    private String notes;

    /** The calendar date on which the approver finalised this decision. */
    private LocalDate date;
}