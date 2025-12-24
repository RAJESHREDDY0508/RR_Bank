package com.RRBank.banking.service;

import com.RRBank.banking.event.PaymentCompletedEvent;
import com.RRBank.banking.event.PaymentFailedEvent;
import com.RRBank.banking.event.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Payment Event Producer
 * Publishes payment-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_EVENTS_TOPIC = "payment.events";

    /**
     * Publish Payment Initiated Event
     */
    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        try {
            log.info("Publishing PaymentInitiatedEvent for paymentId: {}", event.getPaymentId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.getPaymentId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published PaymentInitiatedEvent for paymentId: {} to partition: {}",
                            event.getPaymentId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish PaymentInitiatedEvent for paymentId: {}", 
                            event.getPaymentId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing PaymentInitiatedEvent", e);
        }
    }

    /**
     * Publish Payment Completed Event
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("Publishing PaymentCompletedEvent for paymentId: {}", event.getPaymentId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.getPaymentId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published PaymentCompletedEvent for paymentId: {}",
                            event.getPaymentId());
                } else {
                    log.error("Failed to publish PaymentCompletedEvent for paymentId: {}", 
                            event.getPaymentId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing PaymentCompletedEvent", e);
        }
    }

    /**
     * Publish Payment Failed Event
     */
    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            log.info("Publishing PaymentFailedEvent for paymentId: {}, reason: {}", 
                    event.getPaymentId(), event.getFailureReason());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.getPaymentId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published PaymentFailedEvent for paymentId: {}",
                            event.getPaymentId());
                } else {
                    log.error("Failed to publish PaymentFailedEvent for paymentId: {}", 
                            event.getPaymentId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing PaymentFailedEvent", e);
        }
    }
}
