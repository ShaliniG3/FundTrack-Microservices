package com.cts.fundtrack.identity.controller;

import com.cts.fundtrack.common.dto.AuditRequestDTO;
import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.repository.AuditRepository;
import com.cts.fundtrack.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/internal/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditInternalController {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    @PostMapping("/logs")
    public void receiveAuditLog(@RequestBody AuditRequestDTO dto) {
        log.info("Received internal audit log request from external microservice for user: {}", dto.getUserId());

        User actor = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for auditing"));

        AuditLog logEntry = AuditLog.builder()
                .user(actor)
                .action(dto.getAction())
                .entityId(dto.getEntityId())
                .entityName(dto.getEntityName())
                .timestamp(Instant.now())
                .build();

        auditRepository.save(logEntry);
    }
}