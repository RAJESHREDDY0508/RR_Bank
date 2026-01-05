package com.rrbank.notification.event;

import com.rrbank.notification.entity.Notification;
import com.rrbank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "transaction-completed", groupId = "notification-service")
    public void handleTransactionCompleted(Map<String, Object> event) {
        log.info("Received transaction-completed event: {}", event);
        try {
            String txType = (String) event.get("transactionType");
            Object amount = event.get("amount");
            String toAccountId = (String) event.get("toAccountId");

            if (toAccountId != null) {
                notificationService.createNotification(
                        UUID.fromString(toAccountId),
                        txType + " Completed",
                        "Your " + txType.toLowerCase() + " of $" + amount + " was successful.",
                        Notification.NotificationType.TRANSACTION
                );
            }
        } catch (Exception e) {
            log.error("Failed to process transaction-completed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transaction-failed", groupId = "notification-service")
    public void handleTransactionFailed(Map<String, Object> event) {
        log.info("Received transaction-failed event: {}", event);
        try {
            String txType = (String) event.get("transactionType");
            String reason = (String) event.get("failureReason");
            String fromAccountId = (String) event.get("fromAccountId");

            if (fromAccountId != null) {
                notificationService.createNotification(
                        UUID.fromString(fromAccountId),
                        txType + " Failed",
                        "Your transaction failed: " + reason,
                        Notification.NotificationType.ERROR
                );
            }
        } catch (Exception e) {
            log.error("Failed to process transaction-failed event: {}", e.getMessage());
        }
    }
}
