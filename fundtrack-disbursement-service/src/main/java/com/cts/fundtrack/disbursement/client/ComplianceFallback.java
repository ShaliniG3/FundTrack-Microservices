package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ComplianceFallback implements ComplianceClient {

    @Override
    public boolean isApplicantCompliant(UUID applicationId) {
        log.error("CRITICAL: Compliance Service is unreachable for App ID: {}. Blocking payment for safety.", applicationId);

        // FAIL-CLOSED: Return false.
        // We do not pay if we cannot verify compliance.
        return false;
    }
}