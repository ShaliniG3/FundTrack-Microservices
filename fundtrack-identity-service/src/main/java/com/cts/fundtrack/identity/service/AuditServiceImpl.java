package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.repository.AuditRepository;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;

    @Override
    public void logUserAction(User user, ActionType action, EntityType entityType) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityId(user.getUserId()) // For Auth, the entity is usually the user themselves
                .entityName(entityType)
                .timestamp(Instant.now())
                .build();
                
        auditRepository.save(log);
    }
}