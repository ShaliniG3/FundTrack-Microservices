package com.cts.fundtrack.disbursement.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.models.enums.PaymentFrequency;
import com.cts.fundtrack.disbursement.models.Disbursement;
 
/**
* Service interface for managing grant disbursements and financial allocations.
*/
public interface DisbursementService {
 
    /**
     * Retrieves the disbursement schedule for an application.
     *
     * @param applicationId The unique identifier of the application.
     * @return A list of associated {@link Disbursement} entities.
     */
    List<Disbursement> getScheduleById(UUID applicationId);
 
    /**
     * Calculates the outstanding financial balance for an application.
     *
     * @param applicationId The unique identifier of the application.
     * @return The total outstanding balance.
     */
    Double getRemainingBalance(UUID applicationId);
 
    /**
     * Finalizes program awards and generates payment schedules.
     *
     * @param programId The identifier of the closed program.
     * @param frequency The desired payout frequency.
     * @param numberOfPayments Total installment count per winner.
     * @return A list of generated schedule DTOs.
     */
    List<DisbursementResponseDTO> finalizeAndSplitBudget(UUID programId, PaymentFrequency frequency, int numberOfPayments);
}