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


@Entity
@Table(name = "disbursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "disbursement_id", updatable = false, nullable = false)
    private UUID disbursementId;

    /**
     * Monolith: Application application
     * Microservice: UUID applicationId
     */
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DisbursementStatus status;

    /**
     * Monolith: Payment payment
     * Microservice: UUID paymentId
     */
    @Column(name = "payment_id")
    private UUID paymentId;

}