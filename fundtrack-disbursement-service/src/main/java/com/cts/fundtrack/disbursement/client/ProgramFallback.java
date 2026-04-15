package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ProgramMetadataDTO;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j

public class ProgramFallback implements ProgramClient {

    public ProgramMetadataDTO getProgramById(UUID id) {
        log.error("Program Service is DOWN. Falling back for Program ID: {}", id);

        return ProgramMetadataDTO.builder()
                .programId(id)
                .status("SERVICE_UNAVAILABLE")
                .budget(-1.0) // Signal value to stop the service logic
                .build();
    }
}