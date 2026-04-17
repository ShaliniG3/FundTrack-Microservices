package com.cts.fundtrack.disbursement.models;

import java.time.Instant;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.PaymentMethod;
import com.cts.fundtrack.common.models.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
     * Unique identifier for this payment transaction record, generated as a UUID v4.
     * Exposed to external callers only in AES-encrypted form via
     * {@link com.cts.fundtrack.disbursement.util.EncryptionUtil}.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    /**
     * Logical reference to the parent disbursement installment this transaction settles.
     * The {@code unique = true} constraint enforces a one-payment-per-installment rule,
     * preventing accidental duplicate settlements.
     * Replaces the monolith's {@code Disbursement} entity association.
     */
    @Column(name = "disbursement_id", nullable = false, unique = true)
    private UUID disbursementId;

    /**
     * The monetary value transferred in this transaction, copied from the parent
     * disbursement installment at the time of payment processing.
     */
    @Column(nullable = false)
    private Double amount;

    /**
     * The UTC timestamp at which this payment transaction was recorded.
     * Auto-populated by {@link #onCreate()} if not explicitly set.
     */
    @Column(nullable = false)
    private Instant date;

    /**
     * The channel or mechanism used to transfer the funds
     * (e.g., {@code BANK_TRANSFER}, {@code CHEQUE}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMethod method;

    /**
     * The outcome of this payment transaction (e.g., {@code SUCCESS}, {@code FAILED}).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentStatus status;

    /**
     * JPA lifecycle hook that automatically captures the transaction timestamp
     * immediately before the record is first persisted, if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = Instant.now();
        }
    }
}