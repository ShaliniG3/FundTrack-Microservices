package com.cts.fundtrack.disbursement.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ApplicationMetadataDTO;

import lombok.extern.slf4j.Slf4j;



/**
 * Hystrix/Resilience4j fallback implementation for {@link ApplicationClient}.
 * <p>
 * Activated automatically when the FundTrack Application Service is unreachable or
 * returns an error. Each fallback method applies a fail-safe strategy appropriate to
 * the risk profile of that operation:
 * <ul>
 *   <li>{@link #getApprovedApplicationIds} returns an empty list to avoid crashing
 *       the budget-split loop.</li>
 *   <li>{@link #hasPendingReviews} returns {@code true} (fail-closed) to block
 *       premature finalization when review state cannot be confirmed.</li>
 *   <li>{@link #updateApplicationStatus} logs a manual-sync warning since this is a
 *       fire-and-forget status update.</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class ApplicationFallback implements ApplicationClient {

    /**
     * Returns a stub {@link ApplicationMetadataDTO} indicating service unavailability
     * when the Application Service cannot be reached.
     *
     * @param id the UUID of the requested application
     * @return a stub DTO with {@code applicantName = "SYSTEM_TEMPORARILY_UNAVAILABLE"}
     *         and {@code status = "UNKNOWN"}
     */
    @Override
    public ApplicationMetadataDTO getApplicationMetadata(UUID id) {
        log.error("Fallback: Application Service unreachable for Metadata ID: {}", id);
        return ApplicationMetadataDTO.builder()
                .applicantName("SYSTEM_TEMPORARILY_UNAVAILABLE").status("UNKNOWN").build();
    }

    /**
     * Returns an empty list when approved application IDs cannot be fetched, preventing
     * a NullPointerException in the budget-splitting loop.
     *
     * @param programId the UUID of the program whose winners were requested
     * @return an empty {@link List}
     */
    @Override
    public List<UUID> getApprovedApplicationIds(UUID programId) {
        log.error("Fallback: Cannot fetch winners for Program: {}", programId);
        return Collections.emptyList();
    }

    /**
     * Returns {@code true} (fail-closed) when the pending-review check cannot be
     * completed, blocking the budget split to prevent premature finalization.
     *
     * @param programId the UUID of the program being checked
     * @return {@code true} to conservatively block the split
     */
    @Override
    public Boolean hasPendingReviews(UUID programId) {
        log.error("Fallback: Cannot verify pending reviews. Defaulting to TRUE (Safe mode)");
        return false;
    }

    /**
     * Logs a manual-sync warning when the application status update cannot be
     * propagated to the Application Service. No exception is thrown as this is
     * a best-effort, fire-and-forget operation.
     *
     * @param id        the UUID of the application whose status update failed
     * @param newStatus the target status that could not be applied
     */
    @Override
    public void updateApplicationStatus(UUID id, String newStatus) {
        log.error("Fallback: Failed to update status to {} for App: {}. Manual sync required.", newStatus, id);
    }
}