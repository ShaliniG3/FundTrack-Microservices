package com.fundtrack.audit_service.controller;

import com.fundtrack.audit_service.dto.AuditLogRequestDTO;
import com.fundtrack.audit_service.model.AuditLog;
import com.fundtrack.audit_service.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing and retrieving audit logs.
 * Provides endpoints for logging system actions and querying history by user or entity.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Management", description = "Endpoints for recording and retrieving system audit trails")
public class AuditController {

    private final AuditService auditService;

    /**
     * Records a new audit event in the system.
     *
     * @param request The audit details to be logged.
     * @return 201 Created on success.
     */
    @PostMapping("/log")
    @Operation(summary = "Create a new audit log entry",
            description = "Persists an audit event linked to a specific user and entity action.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Audit log successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data provided")
    })
    public ResponseEntity<Void> createAudit(@RequestBody AuditLogRequestDTO request) {
        auditService.log(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retrieves all audit logs associated with a specific user.
     *
     * @param userId The UUID of the user.
     * @return A list of audit logs for the specified user.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get logs by User ID",
            description = "Returns a chronological list of actions performed by a specific user.")
    public ResponseEntity<List<AuditLog>> getLogsByUser(
            @Parameter(description = "UUID of the user to fetch logs for")
            @PathVariable UUID userId) {
        return ResponseEntity.ok(auditService.getLogsByUser(userId));
    }

    /**
     * Retrieves the history of changes for a specific entity.
     *
     * @param entityId The UUID of the entity (e.g., a specific Project or Account ID).
     * @return A list of audit logs for the specified entity.
     */
    @GetMapping("/entity/{entityId}")
    @Operation(summary = "Get logs by Entity ID",
            description = "Returns the audit trail for a specific resource, showing its lifecycle changes.")
    public ResponseEntity<List<AuditLog>> getLogsByEntity(
            @Parameter(description = "UUID of the entity (resource) to fetch logs for")
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(auditService.getLogsByEntity(entityId));
    }
}