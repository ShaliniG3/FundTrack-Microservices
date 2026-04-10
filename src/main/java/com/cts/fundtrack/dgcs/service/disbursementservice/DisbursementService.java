package com.cts.fundtrack.dgcs.service.disbursementservice;

import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementResponseDTO;

import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.model.enums.PaymentFrequency;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing grant disbursements and strategic financial allocations.
 * <p>
 * This service acts as the orchestration layer for the "Budget Split" algorithm.
 * It manages the transition from a pool of approved applications to a structured
 * financial roadmap, ensuring that total payouts do not exceed the authorized
 * program budget.
 * </p>
 */
public interface DisbursementService {

    /**
     * Retrieves the projected disbursement schedule for a specific application.
     * <p>
     * Serving as the financial source of truth for the applicant's dashboard,
     * this method provides a list of all installments, including their projected
     * dates, amounts, and current statuses (e.g., SCHEDULED, LOCKED, READY).
     * </p>
     *
     * @param applicationId The unique identifier of the grant application.
     * @return A list of associated {@link Disbursement} entities representing the payout roadmap.
     */
    List<Disbursement> getScheduleById(UUID applicationId);

    /**
     * Calculates the current outstanding financial liability for an application.
     * <p>
     * This method aggregates all 'SCHEDULED' and 'LOCKED' installments to determine
     * the total remaining funds yet to be processed for the recipient. It is
     * critical for program-level financial reconciliation.
     * </p>
     *
     * @param applicationId The unique identifier of the grant application.
     * @return The total outstanding balance as a {@link Double}.
     */
    Double getRemainingBalance(UUID applicationId);

    /**
     * Executes the "Budget Split" algorithm to finalize awards and generate installments.
     * <p>
     * This is a critical lifecycle operation triggered when a program is closed.
     * It divides the total program budget equally among all 'APPROVED' winners,
     * generates a time-series of installments based on the specified frequency,
     * and transitions applications to their 'ACCEPTED' state.
     * </p>
     *
     * @param programId        The identifier of the program whose budget is being allocated.
     * @param frequency       The {@link PaymentFrequency} (e.g., MONTHLY, QUARTERLY) for the payouts.
     * @param numberOfPayments The total number of installments to be generated for each winner.
     * @return A list of {@link DisbursementResponseDTO} objects confirming the generated schedules.
     */
    List<DisbursementResponseDTO> finalizeAndSplitBudget(UUID programId, PaymentFrequency frequency, int numberOfPayments);
}