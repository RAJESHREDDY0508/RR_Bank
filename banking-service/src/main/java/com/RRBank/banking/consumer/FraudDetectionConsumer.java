package com.RRBank.banking.consumer;

import com.RRBank.banking.event.TransactionCompletedEvent;
import com.RRBank.banking.service.FraudDetectionService;
import com.RRBank.banking.service.FraudRulesEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Fraud Detection Transaction Consumer
 * Consumes transaction events for real-time fraud analysis
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class FraudDetectionConsumer {

    private final FraudDetectionService fraudDetectionService;
    private final ObjectMapper objectMapper;

    /**
     * Consume transaction events for fraud detection
     */
    @KafkaListener(
            topics = "transaction.events",
            groupId = "fraud-detection-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(String eventJson) {
        try {
            log.info("Received transaction event for fraud analysis: {}", eventJson);

            // Parse event
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            // Only analyze completed transactions
            if ("TRANSACTION_COMPLETED".equals(eventType)) {
                TransactionCompletedEvent event = objectMapper.readValue(
                        eventJson, TransactionCompletedEvent.class);
                
                analyzeTransaction(event);
            }

        } catch (Exception e) {
            log.error("Error processing transaction event for fraud detection", e);
        }
    }

    private void analyzeTransaction(TransactionCompletedEvent event) {
        try {
            log.info("Analyzing transaction {} for fraud", event.getTransactionId());

            // Build transaction context
            FraudRulesEngine.TransactionContext context = FraudRulesEngine.TransactionContext.builder()
                    .transactionId(event.getTransactionId())
                    .accountId(event.getFromAccountId()) // Source account
                    .customerId(null) // Would fetch from account service
                    .amount(event.getAmount())
                    .transactionType(event.getTransactionType())
                    .locationCountry("US") // Mock - would get from transaction metadata
                    .locationCity("New York") // Mock
                    .locationIp("192.168.1.1") // Mock
                    .deviceFingerprint("device-12345") // Mock
                    .build();

            // Analyze for fraud
            fraudDetectionService.analyzeTransaction(context);

        } catch (Exception e) {
            log.error("Error analyzing transaction {} for fraud", event.getTransactionId(), e);
        }
    }
}
