package com.cts.fundtrack.dgcs.client.programclient;

import com.cts.fundtrack.dgcs.client.dto.ProgramMetadataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j

public class ProgramFallback implements ProgramClient {

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