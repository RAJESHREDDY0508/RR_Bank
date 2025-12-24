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
 * Account Events Consumer
 * Consumes events from account.events topic and creates notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AccountEventsConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Consume account events
     */
    @KafkaListener(
            topics = "account.events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountEvent(String eventJson) {
        try {
            log.info("Received account event: {}", eventJson);

            // Parse event to determine type
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "ACCOUNT_CREATED" -> handleAccountCreatedEvent(eventJson);
                case "ACCOUNT_STATUS_CHANGED" -> handleAccountStatusChangedEvent(eventJson);
                case "BALANCE_UPDATED" -> handleBalanceUpdatedEvent(eventJson);
                default -> log.warn("Unknown account event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing account event", e);
        }
    }

    private void handleAccountCreatedEvent(String eventJson) {
        try {
            AccountCreatedEvent event = objectMapper.readValue(eventJson, AccountCreatedEvent.class);
            
            log.info("Processing ACCOUNT_CREATED event for accountId: {}", event.getAccountId());

            // Send notification
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.ACCOUNT_CREATED,
                    Notification.NotificationChannel.EMAIL,
                    "Account Created Successfully",
                    String.format("Your new %s account (%s) has been created successfully with an initial balance of $%s.",
                            event.getAccountType(), event.getAccountNumber(), event.getInitialBalance()),
                    event.getAccountId(),
                    "ACCOUNT"
            );

            // Also send in-app notification
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.ACCOUNT_CREATED,
                    Notification.NotificationChannel.IN_APP,
                    "New Account",
                    "Welcome! Your account is ready to use.",
                    event.getAccountId(),
                    "ACCOUNT"
            );

        } catch (Exception e) {
            log.error("Error handling ACCOUNT_CREATED event", e);
        }
    }

    private void handleAccountStatusChangedEvent(String eventJson) {
        try {
            AccountStatusChangedEvent event = objectMapper.readValue(eventJson, AccountStatusChangedEvent.class);
            
            log.info("Processing ACCOUNT_STATUS_CHANGED event for accountId: {}", event.getAccountId());

            String message = String.format("Your account %s status changed from %s to %s.",
                    event.getAccountNumber(), event.getOldStatus(), event.getNewStatus());
            
            if (event.getReason() != null) {
                message += " Reason: " + event.getReason();
            }

            // Send email notification
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.ACCOUNT_UPDATED,
                    Notification.NotificationChannel.EMAIL,
                    "Account Status Changed",
                    message,
                    event.getAccountId(),
                    "ACCOUNT"
            );

            // If account is frozen or suspended, send urgent SMS
            if ("FROZEN".equals(event.getNewStatus()) || "SUSPENDED".equals(event.getNewStatus())) {
                notificationService.createAndSendNotification(
                        event.getCustomerId(),
                        Notification.NotificationType.SECURITY_ALERT,
                        Notification.NotificationChannel.SMS,
                        "Account Alert",
                        "Your account has been " + event.getNewStatus() + ". Contact us immediately.",
                        event.getAccountId(),
                        "ACCOUNT"
                );
            }

        } catch (Exception e) {
            log.error("Error handling ACCOUNT_STATUS_CHANGED event", e);
        }
    }

    private void handleBalanceUpdatedEvent(String eventJson) {
        try {
            BalanceUpdatedEvent event = objectMapper.readValue(eventJson, BalanceUpdatedEvent.class);
            
            log.info("Processing BALANCE_UPDATED event for accountId: {}", event.getAccountId());

            // Check for low balance (less than $100)
            if (event.getNewBalance().doubleValue() < 100.0 && 
                event.getNewBalance().compareTo(event.getOldBalance()) < 0) {
                
                notificationService.createAndSendNotification(
                        event.getCustomerId(),
                        Notification.NotificationType.BALANCE_LOW,
                        Notification.NotificationChannel.SMS,
                        "Low Balance Alert",
                        String.format("Your account balance is now $%s. Please add funds to avoid fees.",
                                event.getNewBalance()),
                        event.getAccountId(),
                        "ACCOUNT"
                );
            }

            // In-app notification for all balance changes
            String changeType = event.getChangeAmount().doubleValue() > 0 ? "credited with" : "debited";
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.BALANCE_UPDATED,
                    Notification.NotificationChannel.IN_APP,
                    "Balance Update",
                    String.format("Your account was %s $%s. New balance: $%s",
                            changeType, Math.abs(event.getChangeAmount().doubleValue()), event.getNewBalance()),
                    event.getAccountId(),
                    "ACCOUNT"
            );

        } catch (Exception e) {
            log.error("Error handling BALANCE_UPDATED event", e);
        }
    }
}
