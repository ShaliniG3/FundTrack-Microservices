package com.cts.fundtrack.dgcs.model;

import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;


/**
 * Persistence entity representing a planned or executed fund allocation.
 * <p>
 * This model manages the scheduling and execution of grant installments.
 * In this microservice-oriented design, it utilizes UUID references to
 * maintain loose coupling with Applications and Payments while ensuring
 * full traceability across the grant lifecycle.
 * </p>
 */
@Entity
@Table(name = "disbursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disbursement {

    /**
     * Primary key. Uses UUID version 4 to ensure global uniqueness
     * across distributed service nodes.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "disbursement_id", updatable = false, nullable = false)
    private UUID disbursementId;

    /**
     * Logical reference to the parent Grant Application.
     * Essential for validating report eligibility and compliance status.
     */
    @NotNull(message = "Application ID is mandatory")
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    /**
     * Logical reference to the associated Grant Program.
     * Enables program-level budget tracking and financial reporting.
     */
    @NotNull(message = "Program ID is mandatory")
    @Column(name = "program_id", nullable = false)
    private UUID programId;

    /**
     * The specific monetary amount earmarked for this installment.
     */
    @NotNull(message = "Disbursement amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Column(nullable = false)
    private Double amount;

    /**
     * The projected date for fund release.
     */
    @NotNull(message = "Scheduled date is mandatory")
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    /**
     * The actual date the payment was finalized and recorded.
     */
    @Column(name = "actual_date")
    private LocalDate actualDate;

    /**
     * Current state of the installment (e.g., SCHEDULED, PAID, CANCELLED).
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Disbursement status is mandatory")
    @Column(nullable = false, length = 40)
    private DisbursementStatus status;

    /**
     * Logical reference to the realized Payment transaction record.
     * This remains null until the Finance Officer completes reconciliation.
     */
    @Column(name = "payment_id")
    private UUID paymentId;
}