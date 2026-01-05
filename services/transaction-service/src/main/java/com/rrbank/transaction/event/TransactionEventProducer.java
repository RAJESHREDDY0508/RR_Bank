package com.rrbank.transaction.event;

import com.rrbank.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TX_INITIATED_TOPIC = "transaction-initiated";
    private static final String TX_COMPLETED_TOPIC = "transaction-completed";
    private static final String TX_FAILED_TOPIC = "transaction-failed";

    public void publishTransactionInitiated(Transaction tx) {
        publishEvent(TX_INITIATED_TOPIC, tx, "TRANSACTION_INITIATED");
    }

    public void publishTransactionCompleted(Transaction tx) {
        publishEvent(TX_COMPLETED_TOPIC, tx, "TRANSACTION_COMPLETED");
    }

    public void publishTransactionFailed(Transaction tx) {
        publishEvent(TX_FAILED_TOPIC, tx, "TRANSACTION_FAILED");
    }

    private void publishEvent(String topic, Transaction tx, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("transactionId", tx.getId().toString());
            event.put("transactionReference", tx.getTransactionReference());
            event.put("transactionType", tx.getTransactionType().name());
            event.put("fromAccountId", tx.getFromAccountId() != null ? tx.getFromAccountId().toString() : null);
            event.put("toAccountId", tx.getToAccountId() != null ? tx.getToAccountId().toString() : null);
            event.put("amount", tx.getAmount());
            event.put("status", tx.getStatus().name());
            event.put("failureReason", tx.getFailureReason());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(topic, tx.getId().toString(), event);
            log.info("Published {} event for transaction: {}", eventType, tx.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event: {}", eventType, e.getMessage());
        }
    }
}
