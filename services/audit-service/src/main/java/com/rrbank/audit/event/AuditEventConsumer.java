package com.rrbank.audit.event;

import com.rrbank.audit.entity.AuditLog;
import com.rrbank.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(topics = {"transaction-completed", "transaction-failed", "user-created"}, groupId = "audit-service")
    public void handleEvent(Map<String, Object> event) {
        log.info("Received event for audit: {}", event.get("eventType"));
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action((String) event.get("eventType"))
                    .entityType(determineEntityType(event))
                    .entityId(determineEntityId(event))
                    .details(event.toString())
                    .build();
            
            if (event.get("userId") != null) {
                auditLog.setUserId(UUID.fromString((String) event.get("userId")));
            }
            
            auditService.createAuditLog(auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    private String determineEntityType(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if (eventType != null && eventType.contains("TRANSACTION")) return "TRANSACTION";
        if (eventType != null && eventType.contains("USER")) return "USER";
        return "UNKNOWN";
    }

    private String determineEntityId(Map<String, Object> event) {
        if (event.get("transactionId") != null) return (String) event.get("transactionId");
        if (event.get("userId") != null) return (String) event.get("userId");
        return null;
    }
}
