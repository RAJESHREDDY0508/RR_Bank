package com.RRBank.banking.service;

import com.RRBank.banking.event.CustomerCreatedEvent;
import com.RRBank.banking.event.CustomerUpdatedEvent;
import com.RRBank.banking.event.KycVerifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Customer Event Producer
 * Publishes customer-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class CustomerEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CUSTOMER_EVENTS_TOPIC = "customer.events";

    /**
     * Publish Customer Created Event
     */
    public void publishCustomerCreated(CustomerCreatedEvent event) {
        try {
            log.info("Publishing CustomerCreatedEvent for customerId: {}", event.getCustomerId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(CUSTOMER_EVENTS_TOPIC, event.getCustomerId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published CustomerCreatedEvent for customerId: {} to partition: {}",
                            event.getCustomerId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish CustomerCreatedEvent for customerId: {}", 
                            event.getCustomerId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing CustomerCreatedEvent", e);
        }
    }

    /**
     * Publish Customer Updated Event
     */
    public void publishCustomerUpdated(CustomerUpdatedEvent event) {
        try {
            log.info("Publishing CustomerUpdatedEvent for customerId: {}", event.getCustomerId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(CUSTOMER_EVENTS_TOPIC, event.getCustomerId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published CustomerUpdatedEvent for customerId: {}",
                            event.getCustomerId());
                } else {
                    log.error("Failed to publish CustomerUpdatedEvent for customerId: {}", 
                            event.getCustomerId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing CustomerUpdatedEvent", e);
        }
    }

    /**
     * Publish KYC Verified Event
     */
    public void publishKycVerified(KycVerifiedEvent event) {
        try {
            log.info("Publishing KycVerifiedEvent for customerId: {}, status: {}", 
                    event.getCustomerId(), event.getKycStatus());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(CUSTOMER_EVENTS_TOPIC, event.getCustomerId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published KycVerifiedEvent for customerId: {}",
                            event.getCustomerId());
                } else {
                    log.error("Failed to publish KycVerifiedEvent for customerId: {}", 
                            event.getCustomerId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing KycVerifiedEvent", e);
        }
    }
}
