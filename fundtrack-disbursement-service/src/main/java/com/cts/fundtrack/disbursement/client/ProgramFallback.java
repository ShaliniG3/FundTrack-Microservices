package com.cts.fundtrack.disbursement.client;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.cts.fundtrack.common.exceptions.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import com.cts.fundtrack.common.dto.ProgramMetadataDTO;

/**
 * Resilience4j circuit breaker fallback for {@link ProgramClient}.
 *
 * <p>Activated automatically when the FundTrack Program Service is unreachable
 * or returns repeated errors. Rather than returning a sentinel
 * {@link com.cts.fundtrack.common.dto.ProgramMetadataDTO} with
 * {@code status = "SERVICE_UNAVAILABLE"} — which silently poisoned the
 * budget-split status check in
 * {@link com.cts.fundtrack.disbursement.service.DisbursementServiceImpl} and
 * produced a misleading {@code "Budget Split Blocked"} error even when the
 * program was correctly {@code CLOSED} in the database — this fallback throws
 * a {@link ServiceUnavailableException} so the caller receives a clean,
 * typed {@code 503} signal instead of a corrupt program state.</p>
 */
@Component
@Slf4j
public class ProgramFallback implements ProgramClient {

    /**
     * Throws a {@link ServiceUnavailableException} when the Program Service is unavailable.
     *
     * <p>Previously this method returned a stub DTO with {@code status = "SERVICE_UNAVAILABLE"}
     * and {@code budget = -1.0}, intending to abort the budget-split workflow safely.
     * However, this caused {@link com.cts.fundtrack.disbursement.service.DisbursementServiceImpl}
     * to throw {@code "Budget Split Blocked: Program is currently in status SERVICE_UNAVAILABLE"}
     * even when the program's real persisted status was {@code CLOSED} — making the
     * program permanently unusable until the service recovered and the status was manually
     * corrected. Throwing a typed exception instead gives callers an explicit, recoverable
     * signal without mutating or misrepresenting the program's actual state.</p>
     *
     * @param id the UUID of the program that could not be retrieved
     * @throws ServiceUnavailableException always, to signal that the Program Service
     *                                     is currently unreachable
     */
    @Override
    public ProgramMetadataDTO getProgramById(UUID id) {
        log.error("[CircuitBreaker] Program Service unavailable — cannot fetch metadata for programId={}", id);
        throw new ServiceUnavailableException("Program Service is currently unavailable. Please try again later.");
    }
}