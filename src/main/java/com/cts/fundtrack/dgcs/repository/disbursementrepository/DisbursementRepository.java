package com.cts.fundtrack.dgcs.repository.disbursementrepository;

//import com.cts.fundtrack.dgcs.model.Application;
import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing the scheduled installments of a grant[cite: 61, 64].
 */
@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, UUID> {
    long countByApplicationId(UUID applicationId);
    long countByApplicationIdAndStatus(UUID applicationId, DisbursementStatus status);
    @Query("SELECT DISTINCT d.applicationId FROM Disbursement d WHERE d.programId = ?1")
    List<UUID> findDistinctApplicationIdsByProgramId(@Param("programId") UUID programId);
    List<Disbursement> findByApplicationIdOrderByScheduledDateAsc(UUID applicationId);
    List<Disbursement> findByApplicationId(UUID applicationId);
    List<Disbursement> findByStatusAndScheduledDateLessThanEqual(DisbursementStatus status, LocalDate date);
    @Modifying
    @Query("UPDATE Disbursement d SET d.status = 'CANCELLED' " +
            "WHERE d.applicationId = :appId AND d.status = 'SCHEDULED'")
    void cancelFutureInstallments(@Param("appId") UUID appId);
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