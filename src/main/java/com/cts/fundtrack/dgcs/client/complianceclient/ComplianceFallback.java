package com.cts.fundtrack.dgcs.client.complianceclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

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