package com.RRBank.banking.service;

import com.RRBank.banking.event.TransactionCompletedEvent;
import com.RRBank.banking.event.TransactionFailedEvent;
import com.RRBank.banking.event.TransactionInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Transaction Event Producer
 * Publishes transaction-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_EVENTS_TOPIC = "transaction.events";

    /**
     * Publish Transaction Initiated Event
     */
    public void publishTransactionInitiated(TransactionInitiatedEvent event) {
        try {
            log.info("Publishing TransactionInitiatedEvent for transactionId: {}", event.getTransactionId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, event.getTransactionId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published TransactionInitiatedEvent for transactionId: {} to partition: {}",
                            event.getTransactionId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish TransactionInitiatedEvent for transactionId: {}", 
                            event.getTransactionId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing TransactionInitiatedEvent", e);
        }
    }

    /**
     * Publish Transaction Completed Event
     */
    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        try {
            log.info("Publishing TransactionCompletedEvent for transactionId: {}", event.getTransactionId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, event.getTransactionId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published TransactionCompletedEvent for transactionId: {}",
                            event.getTransactionId());
                } else {
                    log.error("Failed to publish TransactionCompletedEvent for transactionId: {}", 
                            event.getTransactionId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing TransactionCompletedEvent", e);
        }
    }

    /**
     * Publish Transaction Failed Event
     */
    public void publishTransactionFailed(TransactionFailedEvent event) {
        try {
            log.info("Publishing TransactionFailedEvent for transactionId: {}, reason: {}", 
                    event.getTransactionId(), event.getFailureReason());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, event.getTransactionId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published TransactionFailedEvent for transactionId: {}",
                            event.getTransactionId());
                } else {
                    log.error("Failed to publish TransactionFailedEvent for transactionId: {}", 
                            event.getTransactionId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing TransactionFailedEvent", e);
        }
    }
}
