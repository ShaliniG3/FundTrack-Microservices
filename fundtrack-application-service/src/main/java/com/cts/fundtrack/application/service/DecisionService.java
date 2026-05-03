package com.cts.fundtrack.application.service;

import com.cts.fundtrack.common.dto.*;
import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the business operations for the approver decision
 * workflow in the FundTrack grant funding system.
 *
 * <p>Implementations manage the terminal stage of the application lifecycle:
 * retrieving applications that are ready for a funding decision, recording an
 * {@code APPROVED} or {@code REJECTED} outcome, and maintaining a history of
 * all decisions made by each approver. Decision mutations also update the parent
 * application status, document verification statuses, and dispatch notifications
 * to the relevant parties.</p>
 *
 * @see DecisionServiceImpl
 */
public interface DecisionService {

    /**
     * Retrieves all grant applications currently in the {@code UNDER_REVIEW} state,
     * forming the approver's pending decision queue.
     *
     * @return a list of {@link ApplicationResponseDTO} objects representing
     *         applications awaiting a final funding decision; empty if none exist
     */
    List<ApplicationResponseDTO> getApplicationsUnderReview();

    /**
     * Retrieves consolidated decision-making details for a specific application,
     * including the reviewer's score and recommendation.
     *
     * @param applicationId the UUID of the application to retrieve
     * @return an {@link ApplicationDecisionDetailsDTO} combining the application
     *         summary, review data, and recommendation
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    ApplicationDecisionDetailsDTO getApplicationById(UUID applicationId);

    /**
     * Records a final funding decision ({@code APPROVED} or {@code REJECTED}) for
     * a grant application and updates all related records accordingly.
     *
     * <p>The application must be in the {@code UNDER_REVIEW} state. On success,
     * the application status and all associated document verification statuses are
     * updated, and notifications are sent to both the applicant and the approver.</p>
     *
     * @param dto the decision payload containing the application ID, approver ID,
     *            outcome string, and optional notes
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotReadyForDecisionException
     *         if the application is not in the {@code UNDER_REVIEW} state
     * @throws com.cts.fundtrack.common.exceptions.InvalidDecisionTypeException if the
     *         decision string is not {@code APPROVED} or {@code REJECTED}
     */
    void processDecision(DecisionRequestDTO dto);

    /**
     * Updates the outcome or notes of an existing decision for a specific application.
     *
     * <p>Both the {@link Decision} record and the parent {@link com.cts.fundtrack.application.model.Application}
     * status are updated to reflect the new outcome. Document verification statuses
     * are also revised accordingly.</p>
     *
     * @param applicationId the UUID of the application whose decision is to be updated
     * @param dto           the updated decision payload with the new outcome and/or notes
     * @throws RuntimeException if no decision record exists for the given application ID
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     * @throws com.cts.fundtrack.common.exceptions.InvalidDecisionTypeException if the
     *         new decision string is not {@code APPROVED} or {@code REJECTED}
     */
    void updateDecision(UUID applicationId, DecisionRequestDTO dto);

    /**
     * Deletes the decision record for a specific application and reverts the
     * application status to {@code UNDER_REVIEW}.
     *
     * <p>All associated document verification statuses are reset to {@code SUBMITTED}.
     * Notifications are dispatched to both the applicant (status revert alert) and
     * the acting staff member (confirmation of revocation).</p>
     *
     * @param applicationId the UUID of the application whose decision is to be deleted
     * @throws RuntimeException if no decision record exists for the given application ID
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    void deleteDecisionByApplicationId(UUID applicationId);

    /**
     * Retrieves the complete history of decisions made by a specific approver.
     *
     * @param approverId the UUID of the approver whose decision history is requested
     * @return an {@link ApproverDashBoardDTO} containing the total count and a list
     *         of individual decision records; empty collections if no decisions found
     */
    ApproverDashBoardDTO getDecisionsByApprover(UUID approverId);
}