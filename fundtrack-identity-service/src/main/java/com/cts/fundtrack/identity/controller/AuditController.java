package com.cts.fundtrack.identity.controller;

import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditRepository auditRepository;

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getByAction(@PathVariable String action) {
        log.info("Fetching audit logs for action: {}", action);
        ActionType actionType = ActionType.valueOf(action);
        return ResponseEntity.ok(auditRepository.findByAction(actionType));
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<AuditLog>> getByEntity(@PathVariable UUID entityId) {
        log.info("Fetching audit logs for entityId: {}", entityId);
        return ResponseEntity.ok(auditRepository.findByEntityId(entityId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getByUser(@PathVariable UUID userId) {
        log.info("Fetching audit logs for userId: {}", userId);
        return ResponseEntity.ok(auditRepository.findByUserUserId(userId));
    }
}