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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    /**
     * Monolith: Disbursement disbursement
     * Microservice: UUID disbursementId
     * 'unique = true' kept to prevent duplicate payments for the same installment.
     */
    @Column(name = "disbursement_id", nullable = false, unique = true)
    private UUID disbursementId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Instant date;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentStatus status;

    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = Instant.now();
        }
    }
}