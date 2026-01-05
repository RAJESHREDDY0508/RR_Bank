package com.rrbank.ledger.event;

import com.rrbank.ledger.entity.LedgerEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class LedgerEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String LEDGER_ENTRY_TOPIC = "ledger-entry-created";
    private static final String BALANCE_UPDATED_TOPIC = "balance-updated";

    @Autowired(required = false)
    public LedgerEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishLedgerEntryCreated(LedgerEntry entry) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping LEDGER_ENTRY_CREATED event for entry: {}", entry.getId());
            return;
        }
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "LEDGER_ENTRY_CREATED");
            event.put("entryId", entry.getId().toString());
            event.put("accountId", entry.getAccountId().toString());
            event.put("transactionId", entry.getTransactionId() != null ? entry.getTransactionId().toString() : null);
            event.put("entryType", entry.getEntryType().name());
            event.put("amount", entry.getAmount());
            event.put("runningBalance", entry.getRunningBalance());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(LEDGER_ENTRY_TOPIC, entry.getAccountId().toString(), event);
            log.info("Published LEDGER_ENTRY_CREATED event for entry: {}", entry.getId());
        } catch (Exception e) {
            log.error("Failed to publish LEDGER_ENTRY_CREATED event: {}", e.getMessage());
        }
    }

    public void publishBalanceUpdated(UUID accountId, BigDecimal newBalance) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping BALANCE_UPDATED event for account: {}", accountId);
            return;
        }
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BALANCE_UPDATED");
            event.put("accountId", accountId.toString());
            event.put("balance", newBalance);
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(BALANCE_UPDATED_TOPIC, accountId.toString(), event);
            log.info("Published BALANCE_UPDATED event for account: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to publish BALANCE_UPDATED event: {}", e.getMessage());
        }
    }
}
