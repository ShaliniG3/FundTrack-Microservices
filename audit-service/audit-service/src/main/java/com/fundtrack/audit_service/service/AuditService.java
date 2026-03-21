package com.fundtrack.audit_service.service;

import com.fundtrack.audit_service.dto.AuditLogRequestDTO;
import com.fundtrack.audit_service.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditService {
    void log(AuditLogRequestDTO request);
    List<AuditLog> getLogsByUser(UUID userId);
    List<AuditLog> getLogsByEntity(UUID entityId);
}