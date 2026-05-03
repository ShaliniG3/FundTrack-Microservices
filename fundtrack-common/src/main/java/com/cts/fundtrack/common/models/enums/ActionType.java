package com.cts.fundtrack.common.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the types of actions that can be performed on system entities.
 * Used to categorize Audit Log entries.
 */
@Schema(description = "Categorization of system actions used for auditing and tracking user activity")
public enum ActionType {

    @Schema(description = "A new record or entity was created in the system")
    CREATE,

    @Schema(description = "An existing record was modified (general data update)")
    UPDATE,

    @Schema(description = "A record or entity was removed from the system")
    DELETE,

    @Schema(description = "A specific transition of an entity's lifecycle status (e.g., DRAFT to ACTIVE)")
    STATUS_CHANGE,

    @Schema(description = "A user successfully authenticated and started a session")
    LOGIN,

    @Schema(description = "A user ended their current session")
    LOGOUT,

    @Schema(description = "A new user account was registered in the system")
    REGISTER,

    @Schema(description = "A user successfully updated or recovered their account password")
    RESET_PASSWORD,
    READ
}