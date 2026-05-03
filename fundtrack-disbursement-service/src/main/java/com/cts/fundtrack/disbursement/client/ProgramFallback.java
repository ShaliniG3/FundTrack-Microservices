package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ProgramMetadataDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Hystrix/Resilience4j fallback implementation for {@link ProgramClient}.
 * <p>
 * Activated automatically when the FundTrack Program Service is unreachable or
 * returns an error. Returns a sentinel {@link ProgramMetadataDTO} with
 * {@code status = "SERVICE_UNAVAILABLE"} and {@code budget = -1.0}, which the
 * {@link com.cts.fundtrack.disbursement.service.DisbursementServiceImpl} interprets
 * as an invalid program state and aborts the budget-split operation safely.
 * </p>
 */
@Component
@Slf4j
public class ProgramFallback implements ProgramClient {

    /**
     * Returns a sentinel {@link ProgramMetadataDTO} indicating service unavailability,
     * causing the disbursement finalization workflow to abort gracefully.
     *
     * @param id the UUID of the program that could not be retrieved
     * @return a stub DTO with {@code status = "SERVICE_UNAVAILABLE"} and
     *         {@code budget = -1.0} as a signal value to stop processing
     */
    @Override
    public ProgramMetadataDTO getProgramById(UUID id) {
        log.error("Program Service is DOWN. Falling back for Program ID: {}", id);

        return ProgramMetadataDTO.builder()
                .programId(id)
                .status("SERVICE_UNAVAILABLE")
                .budget(-1.0) // Signal value to stop the service logic
                .build();
    }
}