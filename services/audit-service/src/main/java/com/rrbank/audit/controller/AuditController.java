package com.rrbank.audit.controller;

import com.rrbank.audit.entity.AuditLog;
import com.rrbank.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @PostMapping
    public ResponseEntity<AuditLog> createAuditLog(@RequestBody AuditLog auditLog) {
        return ResponseEntity.ok(auditService.createAuditLog(auditLog));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLog>> getByUser(@PathVariable UUID userId, Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByUser(userId, pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLog>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByEntity(entityType, entityId, pageable));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<AuditLog>> getByAction(@PathVariable String action, Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByAction(action, pageable));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Audit Service is healthy");
    }
}
