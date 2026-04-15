package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;

public interface AuditService {
    void logUserAction(User user, ActionType action, EntityType entityType);
}