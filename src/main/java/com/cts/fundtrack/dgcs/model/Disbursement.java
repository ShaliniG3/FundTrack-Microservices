package com.cts.fundtrack.dgcs.model;

import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;


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