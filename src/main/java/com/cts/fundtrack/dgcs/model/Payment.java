package com.cts.fundtrack.dgcs.model;

import com.cts.fundtrack.dgcs.model.enums.PaymentMethod;
import com.cts.fundtrack.dgcs.model.enums.PaymentStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence entity representing a realized financial transaction.
 * <p>
 * This model records the actual movement of funds following a disbursement approval.
 * In this microservice-oriented architecture, it maintains a logical link to the
 * parent installment via a disbursement UUID, ensuring strict financial reconciliation
 * and preventing duplicate payments for the same milestone.
 * </p>
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    /**
     * Primary key. Uses UUID version 4 to maintain consistency across the platform.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    /**
     * Logical reference to the Disbursement installment.
     * The unique constraint is critical to enforce the 1:1 ratio between
     * a disbursement schedule and an actual payment record.
     */
    @NotNull(message = "Disbursement reference is mandatory")
    @Column(name = "disbursement_id", nullable = false, unique = true)
    private UUID disbursementId;

    /**
     * The actual monetary value transferred.
     */
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(nullable = false)
    private Double amount;

    /**
     * The timestamp indicating when the transaction was executed.
     */
    @Column(nullable = false, updatable = false)
    private Instant date;

    /**
     * The technical medium used for fund transfer (e.g., BANK_TRANSFER, CHECK).
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Payment method must be specified")
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMethod method;

    /**
     * The current lifecycle status of the transaction (e.g., COMPLETED, FAILED).
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Payment status is mandatory")
    @Column(nullable = false, length = 40)
    private PaymentStatus status;

    /**
     * Lifecycle hook to automatically capture the transaction timestamp
     * if not explicitly provided during the creation phase.
     */
    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = Instant.now();
        }
    }
}