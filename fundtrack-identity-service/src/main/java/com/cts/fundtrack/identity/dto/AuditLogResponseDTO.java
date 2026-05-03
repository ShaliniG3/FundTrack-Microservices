package com.cts.fundtrack.identity.dto;

import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.identity.model.AuditLog;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class AuditLogResponseDTO {

    private final UUID auditId;
    private final ActionType action;
    private final EntityType entityName;
    private final UUID entityId;
    private final String userName;
    private final Instant timestamp;

    public AuditLogResponseDTO(AuditLog log) {
        this.auditId    = log.getAuditId();
        this.action     = log.getAction();
        this.entityName = log.getEntityName();
        this.entityId   = log.getEntityId();
        this.timestamp  = log.getTimestamp();

        // ✅ FIXED: safely get userName without crashing if user is deleted
        String name = "System";
        try {
            if (log.getUser() != null) {
                name = log.getUser().getName();
            }
        } catch (Exception e) {
            name = "Deleted User";
        }
        this.userName = name;
    }
}