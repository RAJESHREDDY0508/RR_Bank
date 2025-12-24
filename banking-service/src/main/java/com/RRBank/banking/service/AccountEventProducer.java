package com.RRBank.banking.service;

import com.RRBank.banking.event.AccountCreatedEvent;
import com.RRBank.banking.event.AccountStatusChangedEvent;
import com.RRBank.banking.event.BalanceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Account Event Producer
 * Publishes account-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AccountEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ACCOUNT_EVENTS_TOPIC = "account.events";

    /**
     * Publish Account Created Event
     */
    public void publishAccountCreated(AccountCreatedEvent event) {
        try {
            log.info("Publishing AccountCreatedEvent for accountId: {}", event.getAccountId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, event.getAccountId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published AccountCreatedEvent for accountId: {} to partition: {}",
                            event.getAccountId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish AccountCreatedEvent for accountId: {}", 
                            event.getAccountId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing AccountCreatedEvent", e);
        }
    }

    /**
     * Publish Account Status Changed Event
     */
    public void publishAccountStatusChanged(AccountStatusChangedEvent event) {
        try {
            log.info("Publishing AccountStatusChangedEvent for accountId: {}, status: {} -> {}",
                    event.getAccountId(), event.getOldStatus(), event.getNewStatus());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, event.getAccountId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published AccountStatusChangedEvent for accountId: {}",
                            event.getAccountId());
                } else {
                    log.error("Failed to publish AccountStatusChangedEvent for accountId: {}", 
                            event.getAccountId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing AccountStatusChangedEvent", e);
        }
    }

    /**
     * Publish Balance Updated Event
     */
    public void publishBalanceUpdated(BalanceUpdatedEvent event) {
        try {
            log.info("Publishing BalanceUpdatedEvent for accountId: {}, change: {} {}",
                    event.getAccountId(), event.getChangeAmount(), event.getTransactionType());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, event.getAccountId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published BalanceUpdatedEvent for accountId: {}",
                            event.getAccountId());
                } else {
                    log.error("Failed to publish BalanceUpdatedEvent for accountId: {}", 
                            event.getAccountId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing BalanceUpdatedEvent", e);
        }
    }
}
