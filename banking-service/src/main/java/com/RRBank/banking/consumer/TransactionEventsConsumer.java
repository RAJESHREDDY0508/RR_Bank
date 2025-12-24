package com.RRBank.banking.consumer;

import com.RRBank.banking.entity.Notification;
import com.RRBank.banking.event.*;
import com.RRBank.banking.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Transaction Events Consumer
 * Consumes events from transaction.events topic and creates notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class TransactionEventsConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Consume transaction events
     */
    @KafkaListener(
            topics = "transaction.events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(String eventJson) {
        try {
            log.info("Received transaction event: {}", eventJson);

            // Parse event to determine type
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "TRANSACTION_INITIATED" -> handleTransactionInitiatedEvent(eventJson);
                case "TRANSACTION_COMPLETED" -> handleTransactionCompletedEvent(eventJson);
                case "TRANSACTION_FAILED" -> handleTransactionFailedEvent(eventJson);
                default -> log.warn("Unknown transaction event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing transaction event", e);
        }
    }

    private void handleTransactionInitiatedEvent(String eventJson) {
        try {
            TransactionInitiatedEvent event = objectMapper.readValue(eventJson, TransactionInitiatedEvent.class);
            
            log.info("Processing TRANSACTION_INITIATED event for transactionId: {}", event.getTransactionId());

            // Send in-app notification
            notificationService.createAndSendNotification(
                    event.getInitiatedBy(),
                    Notification.NotificationType.TRANSACTION_COMPLETED,
                    Notification.NotificationChannel.IN_APP,
                    "Transaction Initiated",
                    String.format("Your %s of $%s has been initiated. Reference: %s",
                            event.getTransactionType(), event.getAmount(), event.getTransactionReference()),
                    event.getTransactionId(),
                    "TRANSACTION"
            );

        } catch (Exception e) {
            log.error("Error handling TRANSACTION_INITIATED event", e);
        }
    }

    private void handleTransactionCompletedEvent(String eventJson) {
        try {
            TransactionCompletedEvent event = objectMapper.readValue(eventJson, TransactionCompletedEvent.class);
            
            log.info("Processing TRANSACTION_COMPLETED event for transactionId: {}", event.getTransactionId());

            // Determine user from account IDs (in production, query account service)
            // For now, we'll send notification to both accounts if it's a transfer

            String message = String.format(
                    "Transaction completed successfully! %s of $%s. Reference: %s",
                    event.getTransactionType(), event.getAmount(), event.getTransactionReference()
            );

            // Send email notification
            notificationService.createAndSendNotification(
                    null, // In production, get userId from account
                    Notification.NotificationType.TRANSACTION_COMPLETED,
                    Notification.NotificationChannel.EMAIL,
                    "Transaction Successful",
                    message,
                    event.getTransactionId(),
                    "TRANSACTION"
            );

            // Send SMS for large transactions (over $1000)
            if (event.getAmount().doubleValue() > 1000.0) {
                notificationService.createAndSendNotification(
                        null, // In production, get userId from account
                        Notification.NotificationType.TRANSACTION_COMPLETED,
                        Notification.NotificationChannel.SMS,
                        "Large Transaction Alert",
                        String.format("Transaction of $%s completed. Ref: %s",
                                event.getAmount(), event.getTransactionReference()),
                        event.getTransactionId(),
                        "TRANSACTION"
                );
            }

            // Always send in-app notification
            notificationService.createAndSendNotification(
                    null, // In production, get userId from account
                    Notification.NotificationType.TRANSACTION_COMPLETED,
                    Notification.NotificationChannel.IN_APP,
                    "Transaction Complete",
                    message,
                    event.getTransactionId(),
                    "TRANSACTION"
            );

        } catch (Exception e) {
            log.error("Error handling TRANSACTION_COMPLETED event", e);
        }
    }

    private void handleTransactionFailedEvent(String eventJson) {
        try {
            TransactionFailedEvent event = objectMapper.readValue(eventJson, TransactionFailedEvent.class);
            
            log.info("Processing TRANSACTION_FAILED event for transactionId: {}", event.getTransactionId());

            String message = String.format(
                    "Transaction failed. %s of $%s could not be completed. Reason: %s. Reference: %s",
                    event.getTransactionType(), event.getAmount(), 
                    event.getFailureReason(), event.getTransactionReference()
            );

            // Send email notification
            notificationService.createAndSendNotification(
                    null, // In production, get userId from account
                    Notification.NotificationType.TRANSACTION_FAILED,
                    Notification.NotificationChannel.EMAIL,
                    "Transaction Failed",
                    message,
                    event.getTransactionId(),
                    "TRANSACTION"
            );

            // Send in-app notification
            notificationService.createAndSendNotification(
                    null, // In production, get userId from account
                    Notification.NotificationType.TRANSACTION_FAILED,
                    Notification.NotificationChannel.IN_APP,
                    "Transaction Failed",
                    message,
                    event.getTransactionId(),
                    "TRANSACTION"
            );

        } catch (Exception e) {
            log.error("Error handling TRANSACTION_FAILED event", e);
        }
    }
}
