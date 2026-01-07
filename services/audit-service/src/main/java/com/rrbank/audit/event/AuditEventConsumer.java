package com.rrbank.audit.event;

import com.rrbank.audit.entity.AuditLog;
import com.rrbank.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(topics = "user-events", groupId = "audit-service-users")
    public void handleUserEvent(Map<String, Object> event) {
        log.info("Received user event for audit: {}", event.get("eventType"));
        processEvent(event, "USER");
    }

    @KafkaListener(topics = "transaction-events", groupId = "audit-service-transactions")
    public void handleTransactionEvent(Map<String, Object> event) {
        log.info("Received transaction event for audit: {}", event.get("eventType"));
        processEvent(event, "TRANSACTION");
    }

    @KafkaListener(topics = "ledger-events", groupId = "audit-service-ledger")
    public void handleLedgerEvent(Map<String, Object> event) {
        log.info("Received ledger event for audit: {}", event.get("eventType"));
        processEvent(event, "LEDGER");
    }

    @KafkaListener(topics = "balance-updated", groupId = "audit-service-balance")
    public void handleBalanceEvent(Map<String, Object> event) {
        log.info("Received balance event for audit: {}", event.get("eventType"));
        processEvent(event, "BALANCE");
    }

    private void processEvent(Map<String, Object> event, String entityType) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action((String) event.get("eventType"))
                    .entityType(entityType)
                    .entityId(determineEntityId(event))
                    .details(event.toString())
                    .build();
            
            if (event.get("userId") != null) {
                try {
                    auditLog.setUserId(UUID.fromString((String) event.get("userId")));
                } catch (Exception e) {
                    log.warn("Could not parse userId: {}", event.get("userId"));
                }
            }
            
            if (event.get("ipAddress") != null) {
                auditLog.setIpAddress((String) event.get("ipAddress"));
            }
            
            auditService.createAuditLog(auditLog);
            log.info("Audit log created for event: {}", event.get("eventType"));
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    private String determineEntityId(Map<String, Object> event) {
        if (event.get("transactionId") != null) return (String) event.get("transactionId");
        if (event.get("entryId") != null) return (String) event.get("entryId");
        if (event.get("accountId") != null) return (String) event.get("accountId");
        if (event.get("userId") != null) return (String) event.get("userId");
        return null;
    }
}
