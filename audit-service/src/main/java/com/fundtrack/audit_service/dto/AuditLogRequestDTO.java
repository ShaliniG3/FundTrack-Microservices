package com.fundtrack.audit_service.dto;

import com.fundtrack.audit_service.model.ActionType;
import com.fundtrack.audit_service.model.EntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request object for capturing system audit events")
public class AuditLogRequestDTO {

    @NotNull(message = "Action type is required")
    @Schema(description = "Type of operation performed", example = "CREATE")
    private ActionType action;

    @NotNull(message = "Entity name is required")
    @Schema(description = "The type of resource being audited", example = "USER")
    private EntityType entityName;

    @NotNull(message = "Entity ID is required")
    @Schema(description = "Unique ID of the affected entity", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID entityId;

    @NotNull(message = "User ID is required")
    @Schema(description = "Unique ID of the user performing the action", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;
}