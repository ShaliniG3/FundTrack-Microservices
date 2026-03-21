package com.fundtrack.audit_service.service;

import com.fundtrack.audit_service.dto.AuditLogRequestDTO;
import com.fundtrack.audit_service.exceptions.EntityIdNotFoundException;
import com.fundtrack.audit_service.exceptions.UserIdNotFoundException;
import com.fundtrack.audit_service.model.AuditLog;
import com.fundtrack.audit_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of the {@link AuditService} responsible for capturing
 * and retrieving system audit logs.
 * <p>
 * This service handles the persistence of audit trails and provides
 * lookup capabilities based on user or entity identifiers.
 */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repository;

    /**
     * Records a new audit entry in the system.
     *
     * @param request The data transfer object containing action details,
     * entity information, and the performing user's ID.
     */
    @Override
    public void log(AuditLogRequestDTO request) {
        AuditLog auditLog = AuditLog.builder()
                .action(request.getAction())
                .entityName(request.getEntityName())
                .entityId(request.getEntityId())
                .userId(request.getUserId())
                .build();
        repository.save(auditLog);
    }

    /**
     * Retrieves all audit logs associated with a specific user.
     *
     * @param userId The unique identifier of the user.
     * @return A list of {@link AuditLog} entries.
     * @throws UserIdNotFoundException if no logs are found for the provided user ID.
     */
    @Override
    public List<AuditLog> getLogsByUser(UUID userId) {
        List<AuditLog> logs = repository.findByUserId(userId);
        if (logs.isEmpty()) {
            throw new UserIdNotFoundException("User not found with Id : " + userId);
        }
        return logs;
    }

    /**
     * Retrieves all audit logs associated with a specific entity (e.g., a transaction or account).
     *
     * @param entityId The unique identifier of the entity.
     * @return A list of {@link AuditLog} entries.
     * @throws EntityIdNotFoundException if no logs are found for the provided entity ID.
     */
    @Override
    public List<AuditLog> getLogsByEntity(UUID entityId) {
        List<AuditLog> logs = repository.findByEntityId(entityId);
        if (logs.isEmpty()) {
            throw new EntityIdNotFoundException("Entity not found with Id : " + entityId);
        }
        return logs;
    }
}