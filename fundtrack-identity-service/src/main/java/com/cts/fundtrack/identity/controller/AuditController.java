package com.cts.fundtrack.identity.controller;

import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit") // This matches your Gateway route
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getUserLogs(@PathVariable UUID userId) {
        // This calls the 'GET' method we added to your AuditService
        return ResponseEntity.ok(auditService.getUserAuditLogs(userId));
    }
    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getLogsByAction(@PathVariable com.cts.fundtrack.common.models.enums.ActionType action) {
        return ResponseEntity.ok(auditService.getLogsByAction(action));
    }
}