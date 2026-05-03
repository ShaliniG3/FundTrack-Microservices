package com.cts.fundtrack.disbursement.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.disbursement.models.Disbursement;

/**
 * Spring Data JPA repository for {@link Disbursement} entities.
 * <p>
 * Provides data access operations for grant disbursement installment records, including
 * schedule retrieval, status-based filtering, balance calculations, and bulk cancellation.
 * Used by the disbursement service layer, the compliance validator, and the nightly
 * scheduler that transitions due installments to {@code PENDING} status.
 * </p>
 */
@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, UUID> {

    /**
     * Counts all disbursement installments associated with a given application,
     * regardless of status. Used to confirm that a funding schedule exists.
     *
     * @param applicationId the UUID of the grant application
     * @return the total number of disbursement records for the application
     */
    long countByApplicationId(UUID applicationId);

    /**
     * Counts disbursement installments for an application filtered by a specific status.
     * Used in compliance gap-analysis (e.g., counting {@code PAID} installments).
     *
     * @param applicationId the UUID of the grant application
     * @param status        the {@link DisbursementStatus} to filter by
     * @return the number of installments matching the given application and status
     */
    long countByApplicationIdAndStatus(UUID applicationId, DisbursementStatus status);

    /**
     * Retrieves the distinct application UUIDs for all disbursements belonging to a
     * given program. Used to build program-level compliance dashboards.
     *
     * @param programId the UUID of the grant program
     * @return a list of distinct application UUIDs that have disbursements under the program
     */
    @Query("SELECT DISTINCT d.applicationId FROM Disbursement d WHERE d.programId = ?1")
    List<UUID> findDistinctApplicationIdsByProgramId(@Param("programId") UUID programId);

    /**
     * Retrieves all disbursement installments for an application ordered chronologically
     * by their scheduled date (ascending). Used to display payment schedules.
     *
     * @param applicationId the UUID of the grant application
     * @return a chronologically ordered list of {@link Disbursement} entities
     */
    List<Disbursement> findByApplicationIdOrderByScheduledDateAsc(UUID applicationId);

    /**
     * Retrieves all disbursement installments for a given application without ordering.
     *
     * @param applicationId the UUID of the grant application
     * @return a list of all {@link Disbursement} entities for the application
     */
    List<Disbursement> findByApplicationId(UUID applicationId);

    /**
     * Retrieves all installments with the given status whose scheduled date falls on
     * or before the specified date. Used by the nightly scheduler to find overdue
     * {@code SCHEDULED} installments that should be transitioned to {@code PENDING}.
     *
     * @param status the {@link DisbursementStatus} to filter by (typically {@code SCHEDULED})
     * @param date   the cutoff date (typically {@code LocalDate.now()})
     * @return a list of due {@link Disbursement} installments
     */
    List<Disbursement> findByStatusAndScheduledDateLessThanEqual(DisbursementStatus status, LocalDate date);

    /**
     * Bulk-cancels all {@code SCHEDULED} installments for a given application.
     * <p>
     * Used when an application becomes non-compliant or is otherwise disqualified
     * from receiving further disbursements. Only affects installments currently in
     * {@code SCHEDULED} status; already-processed installments are left unchanged.
     * </p>
     *
     * @param appId the UUID of the application whose scheduled installments should be cancelled
     */
    @Modifying
    @Query("UPDATE Disbursement d SET d.status = 'CANCELLED' " +
            "WHERE d.applicationId = :appId AND d.status = 'SCHEDULED'")
    void cancelFutureInstallments(@Param("appId") UUID appId);
    /**
     * Retrieves all disbursement installments for a given application.
     * Functionally equivalent to {@link #findByApplicationId(UUID)} but used
     * specifically in the payment history lookup path.
     *
     * @param applicationId the UUID of the grant application
     * @return a list of all {@link Disbursement} entities for the application
     */
    List<Disbursement> findAllByApplicationId(UUID applicationId);
    //    /**
//     * Finds the full payment schedule for a specific grant application[cite: 65,
//     * 98].
//     */
//    List<Disbursement> findByApplication_ApplicationId(UUID applicationId);
//
//    /**
//     * Finds disbursements that are currently 'SCHEDULED' but have a date today or
//     * in the past[cite: 66, 99].
//     * This is the "Engine" for your background scheduler.
//     * * @param status The current status (e.g., SCHEDULED).
//     *
//     * @param date The cutoff date (usually LocalDate.now()).
//     * @return List of installments that need to be transitioned to PENDING.
//     */
//    List<Disbursement> findByStatusAndScheduledDateLessThanEqual(DisbursementStatus status, LocalDate date);
//
//    /**
//     * Filters installments by a specific status.
//     */
//    List<Disbursement> findByStatus(DisbursementStatus status);
//
//    long countByApplication_ApplicationIdAndStatus(UUID applicationId, DisbursementStatus status);
//
//    /**
//     * DESC Logic: Sorts by actualDate descending and picks the first record.
//     * This gives us the most recent payment date.
//     */
//    Optional<Disbursement> findFirstByApplication_ApplicationIdAndStatusOrderByActualDateDesc(UUID applicationId,
//                                                                                              DisbursementStatus status);
//
//    long countByApplicationAndStatus(Application application, DisbursementStatus status);
//    List<Disbursement> findByApplication_ApplicationIdOrderByScheduledDateAsc(UUID applicationId);
//    /**
//     * * Bulk update operation triggered during Just-In-Time compliance check
//     * failures. * Halts financial flow by cancelling all unpaid and non-cancelled
//     * future installments. * * @param appId The UUID of the non-compliant
//     * application. * @return The number of database rows successfully updated to
//     * CANCEL.
//     */
//    @Modifying
//    @Query("UPDATE Disbursement d SET d.status = 'CANCELLED' " + "WHERE d.application.applicationId = :appId "
//            + "AND d.status NOT IN ('PAID', 'CANCELLED')")
//    int cancelFutureInstallments(@Param("appId") UUID appId);

}