package com.RRBank.banking.service;

import com.RRBank.banking.event.StatementGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Statement Event Producer
 * Publishes statement-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class StatementEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String STATEMENT_EVENTS_TOPIC = "statement.events";

    /**
     * Publish Statement Generated Event
     */
    public void publishStatementGenerated(StatementGeneratedEvent event) {
        try {
            log.info("Publishing StatementGeneratedEvent for statementId: {}, period: {}", 
                    event.getStatementId(), event.getStatementPeriod());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(STATEMENT_EVENTS_TOPIC, event.getStatementId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published StatementGeneratedEvent for statementId: {}",
                            event.getStatementId());
                } else {
                    log.error("Failed to publish StatementGeneratedEvent for statementId: {}", 
                            event.getStatementId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing StatementGeneratedEvent", e);
        }
    }
}
