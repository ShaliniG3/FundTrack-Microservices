package com.fundtrack.audit_service.model;
/**
 * Represents the types of actions that can be performed on system entities.
 * Used to categorize Audit Log entries.
 */
public enum ActionType {
    CREATE,         // New record added
    UPDATE,         // Existing record modified
    DELETE,         // Record removed
    STATUS_CHANGE,  // Specific update to the 'status' field
    LOGIN,          // User session started
    LOGOUT,         // User session ended
    REGISTER,
    RESET_PASSWORD

}


