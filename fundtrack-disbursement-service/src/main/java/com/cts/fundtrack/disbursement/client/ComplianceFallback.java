package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Hystrix/Resilience4j fallback implementation for {@link ComplianceClient}.
 * <p>
 * Activated automatically when the FundTrack Compliance Service is unreachable or
 * returns an error. Applies a strict <b>fail-closed</b> strategy: if compliance
 * cannot be verified, disbursement is blocked by returning {@code false}. This
 * prevents funds from being released without a confirmed compliance status.
 * </p>
 */
@Component
@Slf4j
public class ComplianceFallback implements ComplianceClient {

    /**
     * Returns {@code false} (fail-closed) when the Compliance Service is unreachable,
     * blocking any disbursement that depends on a compliance confirmation.
     *
     * @param applicationId the UUID of the application whose compliance could not be verified
     * @return {@code false} to conservatively block payment processing
     */
    @Override
    public boolean isApplicantCompliant(UUID applicationId) {
        log.error("CRITICAL: Compliance Service is unreachable for App ID: {}. Blocking payment for safety.", applicationId);
        // FAIL-CLOSED: We do not pay if we cannot verify compliance.
        return false;
    }
}