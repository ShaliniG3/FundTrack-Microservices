package com.cts.fundtrack.disbursement.models;

import java.time.LocalDate;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.DisbursementStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * JPA entity representing a single grant disbursement installment.
 * <p>
 * A {@code Disbursement} record is one time-sliced portion of a grant award, created
 * during the budget-splitting process when a program is finalized. Each installment
 * tracks its own scheduled date, actual payment date, amount, and lifecycle status
 * (e.g., {@code SCHEDULED}, {@code PENDING}, {@code PAID}, {@code CANCELLED}).
 * </p>
 * <p>
 * In this microservice architecture, relationships to other aggregates (Application,
 * Program, Payment) are maintained as UUID references rather than JPA associations,
 * avoiding cross-service joins and preserving bounded-context isolation.
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
     * Unique identifier for this disbursement installment, generated as a UUID v4.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "disbursement_id", updatable = false, nullable = false)
    private UUID disbursementId;

    /**
     * Logical reference to the parent grant application that this installment funds.
     * Replaces the monolith's {@code Application} entity association.
     */
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    /**
     * Logical reference to the grant program under which this installment was created.
     */
    @Column(name = "program_id", nullable = false)
    private UUID programId;

    /**
     * The monetary value of this individual installment in the program's base currency.
     */
    @Column(nullable = false)
    private Double amount;

    /**
     * The date on which this installment is scheduled to become due for payment.
     * Used by the {@link com.cts.fundtrack.disbursement.service.DisbursementScheduler}
     * to transition the status from {@code SCHEDULED} to {@code PENDING}.
     */
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    /**
     * The date on which this installment was actually paid. Populated when the
     * installment transitions to {@code PAID} status during payment processing.
     * May be {@code null} for unpaid installments.
     */
    @Column(name = "actual_date")
    private LocalDate actualDate;

    /**
     * The current lifecycle state of this installment within the disbursement workflow
     * (e.g., {@code SCHEDULED}, {@code PENDING}, {@code PAID}, {@code CANCELLED}).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DisbursementStatus status;

    /**
     * Logical reference to the {@code Payment} transaction that settled this installment.
     * Replaces the monolith's {@code Payment} entity association. {@code null} until
     * a payment is successfully processed for this installment.
     */
    @Column(name = "payment_id")
    private UUID paymentId;

}