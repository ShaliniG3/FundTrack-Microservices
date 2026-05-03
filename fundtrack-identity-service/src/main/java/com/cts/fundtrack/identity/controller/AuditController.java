package com.cts.fundtrack.identity.controller;

import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.identity.dto.AuditLogResponseDTO;
import com.cts.fundtrack.identity.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogResponseDTO>> getUserLogs(@PathVariable UUID userId) {
        List<AuditLogResponseDTO> result = auditService.getUserAuditLogs(userId)
                .stream()
                .map(AuditLogResponseDTO::new)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLogResponseDTO>> getLogsByAction(@PathVariable ActionType action) {
        List<AuditLogResponseDTO> result = auditService.getLogsByAction(action)
                .stream()
                .map(AuditLogResponseDTO::new)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<AuditLogResponseDTO>> getLogsByEntity(@PathVariable UUID entityId) {
        List<AuditLogResponseDTO> result = auditService.getLogsByEntityId(entityId)
                .stream()
                .map(AuditLogResponseDTO::new)
                .toList();
        return ResponseEntity.ok(result);
    }
}
