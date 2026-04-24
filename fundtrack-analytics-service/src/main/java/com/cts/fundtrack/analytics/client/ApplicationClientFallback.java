package com.cts.fundtrack.analytics.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ApplicationResponseDTO;

@Component
public class ApplicationClientFallback implements ApplicationClient {

    private static final Logger log = LoggerFactory.getLogger(ApplicationClientFallback.class);

    @Override
    public List<ApplicationResponseDTO> getApplicationsByProgram(UUID programId) {
        log.warn("[CircuitBreaker] Application Service unavailable — returning empty applications for programId={}", programId);
        return Collections.emptyList();
    }
}