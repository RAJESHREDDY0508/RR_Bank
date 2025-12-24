package com.RRBank.banking.service;

import com.RRBank.banking.event.FraudAlertEvent;
import com.RRBank.banking.event.TransactionFlaggedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Fraud Event Producer
 * Publishes fraud-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class FraudEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String FRAUD_EVENTS_TOPIC = "fraud.events";

    /**
     * Publish Transaction Flagged Event
     */
    public void publishTransactionFlagged(TransactionFlaggedEvent event) {
        try {
            log.info("Publishing TransactionFlaggedEvent for transactionId: {}, riskScore: {}", 
                    event.getTransactionId(), event.getRiskScore());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(FRAUD_EVENTS_TOPIC, event.getTransactionId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published TransactionFlaggedEvent for transactionId: {}",
                            event.getTransactionId());
                } else {
                    log.error("Failed to publish TransactionFlaggedEvent for transactionId: {}", 
                            event.getTransactionId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing TransactionFlaggedEvent", e);
        }
    }

    /**
     * Publish Fraud Alert Event
     */
    public void publishFraudAlert(FraudAlertEvent event) {
        try {
            log.warn("Publishing FraudAlertEvent for transactionId: {}, riskLevel: {}", 
                    event.getTransactionId(), event.getRiskLevel());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(FRAUD_EVENTS_TOPIC, event.getTransactionId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published FraudAlertEvent for transactionId: {}",
                            event.getTransactionId());
                } else {
                    log.error("Failed to publish FraudAlertEvent for transactionId: {}", 
                            event.getTransactionId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing FraudAlertEvent", e);
        }
    }
}
