package com.cts.fundtrack.analytics.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ProgramResponseDTO;

@Component
public class ProgramClientFallback implements ProgramClient {

    private static final Logger log = LoggerFactory.getLogger(ProgramClientFallback.class);

    @Override
    public ProgramResponseDTO getProgramDetails(UUID programId) {
        log.warn("[CircuitBreaker] Program Service unavailable — returning null for programId={}", programId);
        return null;
    }
}