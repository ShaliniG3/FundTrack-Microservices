package com.fundtrack.analytics_service.client;

import com.fundtrack.analytics_service.dto.applicationdto.ApplicationResponseDTO;
import com.fundtrack.analytics_service.dto.programdto.ProgramResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for communicating with the <b>application-service</b>.
 * <p>
 * This interface defines the contract for retrieving application and program-related
 * data required for analytical processing.
 */
@FeignClient(name = "application-service")
public interface ApplicationClient {

    /**
     * Retrieves a list of all applications associated with a specific program.
     *
     * @param programId The unique identifier (UUID) of the program.
     * @return A {@link List} of {@link ApplicationResponseDTO} objects containing application data.
     */
    @GetMapping("/api/v1/applications/program/{programId}")
    List<ApplicationResponseDTO> getApplicationsByProgram(@PathVariable UUID programId);

    /**
     * Retrieves the core details of a specific program, such as budget and status.
     *
     * @param programId The unique identifier (UUID) of the program to retrieve.
     * @return A {@link ProgramResponseDTO} containing the program's detailed information.
     */
    @GetMapping("/api/v1/programs/{programId}")
    ProgramResponseDTO getProgramDetails(@PathVariable UUID programId);
}