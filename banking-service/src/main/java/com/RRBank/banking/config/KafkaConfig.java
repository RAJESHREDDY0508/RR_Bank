package com.RRBank.banking.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for Phase 4
 * Configures all Kafka topics for RR-Bank microservices
 * 
 * Topics:
 * - account.events: Account creation, updates, status changes
 * - transaction.events: All transaction events
 * - payment.events: Payment processing events
 * - notification.events: Notification delivery events
 * - fraud.events: Fraud detection alerts
 * - statement.events: Statement generation events
 * - audit.events: Audit logging events
 */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    /**
     * Kafka Admin Configuration
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }
    
    // ========================================
    // ACCOUNT EVENTS TOPIC
    // ========================================
    /**
     * Topic for account-related events
     * Events: ACCOUNT_CREATED, ACCOUNT_UPDATED, ACCOUNT_STATUS_CHANGED, BALANCE_UPDATED
     */
    @Bean
    public NewTopic accountEventsTopic() {
        return TopicBuilder.name("account.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "1")
                .build();
    }
    
    // ========================================
    // CUSTOMER EVENTS TOPIC
    // ========================================
    /**
     * Topic for customer-related events
     * Events: CUSTOMER_CREATED, CUSTOMER_UPDATED, KYC_VERIFIED
     */
    @Bean
    public NewTopic customerEventsTopic() {
        return TopicBuilder.name("customer.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .build();
    }
    
    // ========================================
    // TRANSACTION EVENTS TOPIC
    // ========================================
    /**
     * Topic for transaction events
     * Events: TRANSACTION_INITIATED, TRANSACTION_COMPLETED, TRANSACTION_FAILED, TRANSACTION_REVERSED
     * High volume topic - more partitions for better throughput
     */
    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name("transaction.events")
                .partitions(6)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("compression.type", "snappy")
                .config("segment.ms", "86400000") // 1 day
                .config("min.insync.replicas", "1")
                .build();
    }
    
    // ========================================
    // PAYMENT EVENTS TOPIC
    // ========================================
    /**
     * Topic for payment events
     * Events: PAYMENT_INITIATED, PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_SCHEDULED
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("compression.type", "snappy")
                .build();
    }
    
    // ========================================
    // NOTIFICATION EVENTS TOPIC
    // ========================================
    /**
     * Topic for notification events
     * Events: NOTIFICATION_CREATED, NOTIFICATION_SENT, NOTIFICATION_FAILED
     */
    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name("notification.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .build();
    }
    
    // ========================================
    // FRAUD ALERTS TOPIC
    // ========================================
    /**
     * Topic for fraud detection alerts
     * Events: FRAUD_DETECTED, FRAUD_RESOLVED, RISK_SCORE_CALCULATED
     * Critical topic - longer retention
     */
    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name("fraud.alerts")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "7776000000") // 90 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "1")
                .build();
    }
    
    // ========================================
    // STATEMENT EVENTS TOPIC
    // ========================================
    /**
     * Topic for statement generation events
     * Events: STATEMENT_GENERATED, STATEMENT_REQUESTED
     */
    @Bean
    public NewTopic statementEventsTopic() {
        return TopicBuilder.name("statement.events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("compression.type", "snappy")
                .build();
    }
    
    // ========================================
    // AUDIT EVENTS TOPIC
    // ========================================
    /**
     * Topic for audit logging events
     * Events: All system audit events
     * Critical topic - longest retention, most partitions for high volume
     */
    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit.events")
                .partitions(6)
                .replicas(1)
                .config("retention.ms", "31536000000") // 1 year
                .config("compression.type", "snappy")
                .config("segment.ms", "86400000") // 1 day
                .config("min.insync.replicas", "1")
                .build();
    }
    
    // ========================================
    // DEAD LETTER QUEUE TOPICS
    // ========================================
    /**
     * Topic for failed messages
     * Used for messages that fail processing after retries
     */
    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("dlq.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "7776000000") // 90 days
                .config("compression.type", "snappy")
                .build();
    }
    
    // ========================================
    // RETRY TOPICS
    // ========================================
    /**
     * Topic for retry messages
     * Used for messages that need to be retried
     */
    @Bean
    public NewTopic retryTopic() {
        return TopicBuilder.name("retry.events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .build();
    }
}
