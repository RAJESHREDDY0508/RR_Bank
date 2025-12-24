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
 * Payment Events Consumer
 * Consumes events from payment.events topic and creates notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class PaymentEventsConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Consume payment events
     */
    @KafkaListener(
            topics = "payment.events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(String eventJson) {
        try {
            log.info("Received payment event: {}", eventJson);

            // Parse event to determine type
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "PAYMENT_INITIATED" -> handlePaymentInitiatedEvent(eventJson);
                case "PAYMENT_COMPLETED" -> handlePaymentCompletedEvent(eventJson);
                case "PAYMENT_FAILED" -> handlePaymentFailedEvent(eventJson);
                default -> log.warn("Unknown payment event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing payment event", e);
        }
    }

    private void handlePaymentInitiatedEvent(String eventJson) {
        try {
            PaymentInitiatedEvent event = objectMapper.readValue(eventJson, PaymentInitiatedEvent.class);
            
            log.info("Processing PAYMENT_INITIATED event for paymentId: {}", event.getPaymentId());

            // Send in-app notification
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.PAYMENT_SCHEDULED,
                    Notification.NotificationChannel.IN_APP,
                    "Payment Initiated",
                    String.format("Your payment of $%s to %s has been initiated. Reference: %s",
                            event.getAmount(), event.getPayeeName(), event.getPaymentReference()),
                    event.getPaymentId(),
                    "PAYMENT"
            );

        } catch (Exception e) {
            log.error("Error handling PAYMENT_INITIATED event", e);
        }
    }

    private void handlePaymentCompletedEvent(String eventJson) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(eventJson, PaymentCompletedEvent.class);
            
            log.info("Processing PAYMENT_COMPLETED event for paymentId: {}", event.getPaymentId());

            String message = String.format(
                    "Payment successful! $%s to %s has been processed. Reference: %s",
                    event.getAmount(), event.getPayeeName(), event.getPaymentReference()
            );

            // Send email confirmation
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.EMAIL,
                    "Payment Confirmation",
                    message,
                    event.getPaymentId(),
                    "PAYMENT"
            );

            // Send SMS for bill payments
            if ("BILL".equals(event.getPaymentType())) {
                notificationService.createAndSendNotification(
                        event.getCustomerId(),
                        Notification.NotificationType.PAYMENT_COMPLETED,
                        Notification.NotificationChannel.SMS,
                        "Bill Payment Successful",
                        String.format("Bill payment of $%s to %s completed. Ref: %s",
                                event.getAmount(), event.getPayeeName(), event.getPaymentReference()),
                        event.getPaymentId(),
                        "PAYMENT"
                );
            }

            // In-app notification
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.PAYMENT_COMPLETED,
                    Notification.NotificationChannel.IN_APP,
                    "Payment Complete",
                    message,
                    event.getPaymentId(),
                    "PAYMENT"
            );

        } catch (Exception e) {
            log.error("Error handling PAYMENT_COMPLETED event", e);
        }
    }

    private void handlePaymentFailedEvent(String eventJson) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(eventJson, PaymentFailedEvent.class);
            
            log.info("Processing PAYMENT_FAILED event for paymentId: {}", event.getPaymentId());

            String message = String.format(
                    "Payment failed. Your payment of $%s to %s could not be completed. Reason: %s. Reference: %s",
                    event.getAmount(), event.getPayeeName(), 
                    event.getFailureReason(), event.getPaymentReference()
            );

            // Send email notification
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.PAYMENT_FAILED,
                    Notification.NotificationChannel.EMAIL,
                    "Payment Failed",
                    message,
                    event.getPaymentId(),
                    "PAYMENT"
            );

            // Send SMS alert
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.PAYMENT_FAILED,
                    Notification.NotificationChannel.SMS,
                    "Payment Failed",
                    String.format("Payment of $%s to %s failed: %s",
                            event.getAmount(), event.getPayeeName(), event.getFailureReason()),
                    event.getPaymentId(),
                    "PAYMENT"
            );

            // In-app notification
            notificationService.createAndSendNotification(
                    event.getCustomerId(),
                    Notification.NotificationType.PAYMENT_FAILED,
                    Notification.NotificationChannel.IN_APP,
                    "Payment Failed",
                    message,
                    event.getPaymentId(),
                    "PAYMENT"
            );

        } catch (Exception e) {
            log.error("Error handling PAYMENT_FAILED event", e);
        }
    }
}
