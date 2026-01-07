package com.rrbank.transaction.event;

import com.rrbank.transaction.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";

    @Autowired(required = false)
    public TransactionEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTransactionInitiated(Transaction tx) {
        publishEvent(tx, "TRANSACTION_INITIATED");
    }

    public void publishTransactionCompleted(Transaction tx) {
        publishEvent(tx, "TRANSACTION_COMPLETED");
    }

    public void publishTransactionFailed(Transaction tx) {
        publishEvent(tx, "TRANSACTION_FAILED");
    }

    private void publishEvent(Transaction tx, String eventType) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping {} event for transaction: {}", eventType, tx.getId());
            return;
        }
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("transactionId", tx.getId().toString());
            event.put("transactionReference", tx.getTransactionReference());
            event.put("transactionType", tx.getTransactionType().name());
            event.put("fromAccountId", tx.getFromAccountId() != null ? tx.getFromAccountId().toString() : null);
            event.put("toAccountId", tx.getToAccountId() != null ? tx.getToAccountId().toString() : null);
            event.put("amount", tx.getAmount());
            event.put("currency", tx.getCurrency());
            event.put("status", tx.getStatus().name());
            event.put("failureReason", tx.getFailureReason());
            event.put("description", tx.getDescription());
            event.put("initiatedBy", tx.getInitiatedBy() != null ? tx.getInitiatedBy().toString() : null);
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, tx.getId().toString(), event);
            log.info("Published {} event for transaction: {}", eventType, tx.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event: {}", eventType, e.getMessage());
        }
    }
}
