package com.RRBank.banking.consumer;

import com.RRBank.banking.entity.AuditLog;
import com.RRBank.banking.event.*;
import com.RRBank.banking.service.AuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit Events Consumer
 * Consumes ALL Kafka topics and creates immutable audit logs
 * This is the centralized audit logging service
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AuditEventsConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Consume account events
     */
    @KafkaListener(
            topics = "account.events",
            groupId = "audit-service-account-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAccountEvent(String eventJson) {
        try {
            log.info("Audit Service received account event");
            
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "ACCOUNT_CREATED" -> auditAccountCreatedEvent(eventJson);
                case "ACCOUNT_STATUS_CHANGED" -> auditAccountStatusChangedEvent(eventJson);
                case "BALANCE_UPDATED" -> auditBalanceUpdatedEvent(eventJson);
                default -> auditGenericEvent(eventJson, "account.events", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing account event for audit", e);
        }
    }

    /**
     * Consume customer events
     */
    @KafkaListener(
            topics = "customer.events",
            groupId = "audit-service-customer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCustomerEvent(String eventJson) {
        try {
            log.info("Audit Service received customer event");
            
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "CUSTOMER_CREATED" -> auditCustomerCreatedEvent(eventJson);
                case "CUSTOMER_UPDATED" -> auditCustomerUpdatedEvent(eventJson);
                case "KYC_VERIFIED" -> auditKycVerifiedEvent(eventJson);
                default -> auditGenericEvent(eventJson, "customer.events", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing customer event for audit", e);
        }
    }

    /**
     * Consume transaction events
     */
    @KafkaListener(
            topics = "transaction.events",
            groupId = "audit-service-transaction-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(String eventJson) {
        try {
            log.info("Audit Service received transaction event");
            
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "TRANSACTION_INITIATED" -> auditTransactionInitiatedEvent(eventJson);
                case "TRANSACTION_COMPLETED" -> auditTransactionCompletedEvent(eventJson);
                case "TRANSACTION_FAILED" -> auditTransactionFailedEvent(eventJson);
                case "TRANSACTION_FLAGGED" -> auditTransactionFlaggedEvent(eventJson);
                default -> auditGenericEvent(eventJson, "transaction.events", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing transaction event for audit", e);
        }
    }

    /**
     * Consume payment events
     */
    @KafkaListener(
            topics = "payment.events",
            groupId = "audit-service-payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(String eventJson) {
        try {
            log.info("Audit Service received payment event");
            
            var eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "PAYMENT_INITIATED" -> auditPaymentInitiatedEvent(eventJson);
                case "PAYMENT_COMPLETED" -> auditPaymentCompletedEvent(eventJson);
                case "PAYMENT_FAILED" -> auditPaymentFailedEvent(eventJson);
                default -> auditGenericEvent(eventJson, "payment.events", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing payment event for audit", e);
        }
    }

    /**
     * Consume fraud detection events
     */
    @KafkaListener(
            topics = "fraud.alerts",
            groupId = "audit-service-fraud-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFraudAlert(String eventJson) {
        try {
            log.info("Audit Service received fraud alert");
            auditFraudAlertEvent(eventJson);

        } catch (Exception e) {
            log.error("Error processing fraud alert for audit", e);
        }
    }

    /**
     * Consume statement events
     */
    @KafkaListener(
            topics = "statement.events",
            groupId = "audit-service-statement-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStatementEvent(String eventJson) {
        try {
            log.info("Audit Service received statement event");
            auditStatementGeneratedEvent(eventJson);

        } catch (Exception e) {
            log.error("Error processing statement event for audit", e);
        }
    }

    // ==================== Audit Event Handlers ====================

    private void auditAccountCreatedEvent(String eventJson) throws Exception {
        AccountCreatedEvent event = objectMapper.readValue(eventJson, AccountCreatedEvent.class);
        
        auditService.createAuditLog(
                "ACCOUNT_CREATED",
                "AccountService",
                AuditLog.Severity.INFO,
                "ACCOUNT",
                event.getAccountId(),
                null,
                event.getCustomerId(),
                event.getAccountId(),
                "CREATE_ACCOUNT",
                String.format("New %s account created with number %s and initial balance $%s",
                        event.getAccountType(), event.getAccountNumber(), event.getInitialBalance()),
                null,
                objectMapper.writeValueAsString(event),
                null,
                objectMapper.writeValueAsString(event),
                false,
                true // Compliance flag for new account creation
        );
    }

    private void auditAccountStatusChangedEvent(String eventJson) throws Exception {
        AccountStatusChangedEvent event = objectMapper.readValue(eventJson, AccountStatusChangedEvent.class);
        
        AuditLog.Severity severity = "FROZEN".equals(event.getNewStatus()) || 
                                      "SUSPENDED".equals(event.getNewStatus()) ? 
                                      AuditLog.Severity.WARNING : AuditLog.Severity.INFO;
        
        auditService.createAuditLog(
                "ACCOUNT_STATUS_CHANGED",
                "AccountService",
                severity,
                "ACCOUNT",
                event.getAccountId(),
                null,
                event.getCustomerId(),
                event.getAccountId(),
                "STATUS_CHANGE",
                String.format("Account %s status changed from %s to %s. Reason: %s",
                        event.getAccountNumber(), event.getOldStatus(), event.getNewStatus(), event.getReason()),
                event.getOldStatus(),
                event.getNewStatus(),
                null,
                objectMapper.writeValueAsString(event),
                false,
                true // Compliance flag for status changes
        );
    }

    private void auditBalanceUpdatedEvent(String eventJson) throws Exception {
        BalanceUpdatedEvent event = objectMapper.readValue(eventJson, BalanceUpdatedEvent.class);
        
        auditService.createAuditLog(
                "BALANCE_UPDATED",
                "AccountService",
                AuditLog.Severity.INFO,
                "ACCOUNT",
                event.getAccountId(),
                null,
                event.getCustomerId(),
                event.getAccountId(),
                "BALANCE_UPDATE",
                String.format("Balance updated by $%s. New balance: $%s",
                        event.getChangeAmount(), event.getNewBalance()),
                event.getOldBalance().toString(),
                event.getNewBalance().toString(),
                null,
                objectMapper.writeValueAsString(event),
                false,
                false
        );
    }

    private void auditCustomerCreatedEvent(String eventJson) throws Exception {
        CustomerCreatedEvent event = objectMapper.readValue(eventJson, CustomerCreatedEvent.class);
        
        auditService.createAuditLog(
                "CUSTOMER_CREATED",
                "CustomerService",
                AuditLog.Severity.INFO,
                "CUSTOMER",
                event.getCustomerId(),
                null,
                event.getCustomerId(),
                null,
                "CREATE_CUSTOMER",
                String.format("New customer created: %s %s with email %s",
                        event.getFirstName(), event.getLastName(), event.getEmail()),
                null,
                objectMapper.writeValueAsString(event),
                null,
                objectMapper.writeValueAsString(event),
                true, // Sensitive - contains PII
                true // Compliance flag for new customer
        );
    }

    private void auditCustomerUpdatedEvent(String eventJson) throws Exception {
        CustomerUpdatedEvent event = objectMapper.readValue(eventJson, CustomerUpdatedEvent.class);
        
        auditService.createAuditLog(
                "CUSTOMER_UPDATED",
                "CustomerService",
                AuditLog.Severity.INFO,
                "CUSTOMER",
                event.getCustomerId(),
                null,
                event.getCustomerId(),
                null,
                "UPDATE_CUSTOMER",
                "Customer profile updated",
                null,
                null,
                null,
                objectMapper.writeValueAsString(event),
                true, // Sensitive - contains PII
                true // Compliance flag
        );
    }

    private void auditKycVerifiedEvent(String eventJson) throws Exception {
        KycVerifiedEvent event = objectMapper.readValue(eventJson, KycVerifiedEvent.class);
        
        auditService.createAuditLog(
                "KYC_VERIFIED",
                "CustomerService",
                AuditLog.Severity.INFO,
                "CUSTOMER",
                event.getCustomerId(),
                null,
                event.getCustomerId(),
                null,
                "KYC_VERIFICATION",
                String.format("KYC verification completed. Status: %s", event.getKycStatus()),
                null,
                event.getKycStatus(),
                null,
                objectMapper.writeValueAsString(event),
                true, // Sensitive - compliance data
                true // Compliance flag
        );
    }

    private void auditTransactionInitiatedEvent(String eventJson) throws Exception {
        TransactionInitiatedEvent event = objectMapper.readValue(eventJson, TransactionInitiatedEvent.class);
        
        auditService.createAuditLog(
                "TRANSACTION_INITIATED",
                "TransactionService",
                AuditLog.Severity.INFO,
                "TRANSACTION",
                event.getTransactionId(),
                null,
                null,
                event.getAccountId(),
                "INITIATE_TRANSACTION",
                String.format("%s transaction of $%s initiated",
                        event.getTransactionType(), event.getAmount()),
                null,
                objectMapper.writeValueAsString(event),
                null,
                objectMapper.writeValueAsString(event),
                false,
                false
        );
    }

    private void auditTransactionCompletedEvent(String eventJson) throws Exception {
        TransactionCompletedEvent event = objectMapper.readValue(eventJson, TransactionCompletedEvent.class);
        
        auditService.createAuditLog(
                "TRANSACTION_COMPLETED",
                "TransactionService",
                AuditLog.Severity.INFO,
                "TRANSACTION",
                event.getTransactionId(),
                null,
                null,
                event.getAccountId(),
                "COMPLETE_TRANSACTION",
                String.format("Transaction completed successfully. Amount: $%s, New balance: $%s",
                        event.getAmount(), event.getNewBalance()),
                null,
                event.getStatus(),
                null,
                objectMapper.writeValueAsString(event),
                false,
                false
        );
    }

    private void auditTransactionFailedEvent(String eventJson) throws Exception {
        TransactionFailedEvent event = objectMapper.readValue(eventJson, TransactionFailedEvent.class);
        
        auditService.createAuditLog(
                "TRANSACTION_FAILED",
                "TransactionService",
                AuditLog.Severity.ERROR,
                "TRANSACTION",
                event.getTransactionId(),
                null,
                null,
                event.getAccountId(),
                "FAIL_TRANSACTION",
                String.format("Transaction failed. Amount: $%s, Reason: %s",
                        event.getAmount(), event.getReason()),
                null,
                event.getStatus(),
                null,
                objectMapper.writeValueAsString(event),
                false,
                true // Compliance flag for failed transactions
        );
    }

    private void auditTransactionFlaggedEvent(String eventJson) throws Exception {
        TransactionFlaggedEvent event = objectMapper.readValue(eventJson, TransactionFlaggedEvent.class);
        
        auditService.createAuditLog(
                "TRANSACTION_FLAGGED",
                "FraudDetectionService",
                AuditLog.Severity.WARNING,
                "TRANSACTION",
                event.getTransactionId(),
                null,
                null,
                event.getAccountId(),
                "FLAG_TRANSACTION",
                String.format("Transaction flagged for review. Risk Score: %s, Reason: %s",
                        event.getRiskScore(), event.getFraudReason()),
                null,
                objectMapper.writeValueAsString(event),
                null,
                objectMapper.writeValueAsString(event),
                false,
                true // Compliance flag
        );
    }

    private void auditPaymentInitiatedEvent(String eventJson) throws Exception {
        PaymentInitiatedEvent event = objectMapper.readValue(eventJson, PaymentInitiatedEvent.class);
        
        auditService.createAuditLog(
                "PAYMENT_INITIATED",
                "PaymentService",
                AuditLog.Severity.INFO,
                "PAYMENT",
                event.getPaymentId(),
                null,
                null,
                event.getAccountId(),
                "INITIATE_PAYMENT",
                String.format("%s payment of $%s initiated",
                        event.getPaymentType(), event.getAmount()),
                null,
                objectMapper.writeValueAsString(event),
                null,
                objectMapper.writeValueAsString(event),
                false,
                false
        );
    }

    private void auditPaymentCompletedEvent(String eventJson) throws Exception {
        PaymentCompletedEvent event = objectMapper.readValue(eventJson, PaymentCompletedEvent.class);
        
        auditService.createAuditLog(
                "PAYMENT_COMPLETED",
                "PaymentService",
                AuditLog.Severity.INFO,
                "PAYMENT",
                event.getPaymentId(),
                null,
                null,
                event.getAccountId(),
                "COMPLETE_PAYMENT",
                String.format("Payment completed successfully. Amount: $%s",
                        event.getAmount()),
                null,
                event.getStatus(),
                null,
                objectMapper.writeValueAsString(event),
                false,
                false
        );
    }

    private void auditPaymentFailedEvent(String eventJson) throws Exception {
        PaymentFailedEvent event = objectMapper.readValue(eventJson, PaymentFailedEvent.class);
        
        auditService.createAuditLog(
                "PAYMENT_FAILED",
                "PaymentService",
                AuditLog.Severity.ERROR,
                "PAYMENT",
                event.getPaymentId(),
                null,
                null,
                event.getAccountId(),
                "FAIL_PAYMENT",
                String.format("Payment failed. Amount: $%s, Reason: %s",
                        event.getAmount(), event.getReason()),
                null,
                event.getStatus(),
                null,
                objectMapper.writeValueAsString(event),
                false,
                true // Compliance flag
        );
    }

    private void auditFraudAlertEvent(String eventJson) throws Exception {
        FraudAlertEvent event = objectMapper.readValue(eventJson, FraudAlertEvent.class);
        
        auditService.createAuditLog(
                "FRAUD_ALERT",
                "FraudDetectionService",
                AuditLog.Severity.CRITICAL,
                "FRAUD",
                event.getFraudEventId(),
                null,
                event.getCustomerId(),
                event.getAccountId(),
                "FRAUD_DETECTED",
                String.format("Fraud detected! Type: %s, Risk Level: %s, Amount: $%s",
                        event.getFraudType(), event.getRiskLevel(), event.getAmount()),
                null,
                objectMapper.writeValueAsString(event),
                null,
                objectMapper.writeValueAsString(event),
                true, // Sensitive
                true // Compliance flag
        );
    }

    private void auditStatementGeneratedEvent(String eventJson) throws Exception {
        StatementGeneratedEvent event = objectMapper.readValue(eventJson, StatementGeneratedEvent.class);
        
        auditService.createAuditLog(
                "STATEMENT_GENERATED",
                "StatementService",
                AuditLog.Severity.INFO,
                "STATEMENT",
                event.getStatementId(),
                null,
                event.getCustomerId(),
                event.getAccountId(),
                "GENERATE_STATEMENT",
                String.format("Statement generated for period %s to %s",
                        event.getStartDate(), event.getEndDate()),
                null,
                objectMapper.writeValueAsString(event),
                null,
                objectMapper.writeValueAsString(event),
                false,
                false
        );
    }

    private void auditGenericEvent(String eventJson, String topic, String eventType) throws Exception {
        JsonNode eventNode = objectMapper.readTree(eventJson);
        
        auditService.createAuditLog(
                eventType,
                topic,
                AuditLog.Severity.INFO,
                "UNKNOWN",
                null,
                null,
                null,
                null,
                "EVENT_CAPTURED",
                String.format("Generic event captured: %s from topic %s", eventType, topic),
                null,
                null,
                null,
                eventJson,
                false,
                false
        );
    }
}
