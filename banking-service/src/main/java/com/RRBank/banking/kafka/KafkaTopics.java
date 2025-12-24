package com.RRBank.banking.kafka;

/**
 * Kafka Topics Constants
 * Centralized constants for all Kafka topics and event types
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    // ========================================
    // TOPIC NAMES
    // ========================================
    
    public static final String ACCOUNT_EVENTS = "account.events";
    public static final String CUSTOMER_EVENTS = "customer.events";
    public static final String TRANSACTION_EVENTS = "transaction.events";
    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String NOTIFICATION_EVENTS = "notification.events";
    public static final String FRAUD_ALERTS = "fraud.alerts";
    public static final String STATEMENT_EVENTS = "statement.events";
    public static final String AUDIT_EVENTS = "audit.events";
    public static final String DLQ_EVENTS = "dlq.events";
    public static final String RETRY_EVENTS = "retry.events";

    // ========================================
    // ACCOUNT EVENT TYPES
    // ========================================
    
    public static final class AccountEvents {
        public static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";
        public static final String ACCOUNT_UPDATED = "ACCOUNT_UPDATED";
        public static final String ACCOUNT_STATUS_CHANGED = "ACCOUNT_STATUS_CHANGED";
        public static final String BALANCE_UPDATED = "BALANCE_UPDATED";
        public static final String ACCOUNT_CLOSED = "ACCOUNT_CLOSED";
        
        private AccountEvents() {}
    }

    // ========================================
    // CUSTOMER EVENT TYPES
    // ========================================
    
    public static final class CustomerEvents {
        public static final String CUSTOMER_CREATED = "CUSTOMER_CREATED";
        public static final String CUSTOMER_UPDATED = "CUSTOMER_UPDATED";
        public static final String KYC_VERIFIED = "KYC_VERIFIED";
        public static final String KYC_FAILED = "KYC_FAILED";
        public static final String CUSTOMER_SUSPENDED = "CUSTOMER_SUSPENDED";
        public static final String CUSTOMER_ACTIVATED = "CUSTOMER_ACTIVATED";
        
        private CustomerEvents() {}
    }

    // ========================================
    // TRANSACTION EVENT TYPES
    // ========================================
    
    public static final class TransactionEvents {
        public static final String TRANSACTION_INITIATED = "TRANSACTION_INITIATED";
        public static final String TRANSACTION_COMPLETED = "TRANSACTION_COMPLETED";
        public static final String TRANSACTION_FAILED = "TRANSACTION_FAILED";
        public static final String TRANSACTION_REVERSED = "TRANSACTION_REVERSED";
        public static final String TRANSACTION_FLAGGED = "TRANSACTION_FLAGGED";
        public static final String TRANSACTION_VALIDATED = "TRANSACTION_VALIDATED";
        
        private TransactionEvents() {}
    }

    // ========================================
    // PAYMENT EVENT TYPES
    // ========================================
    
    public static final class PaymentEvents {
        public static final String PAYMENT_INITIATED = "PAYMENT_INITIATED";
        public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String PAYMENT_SCHEDULED = "PAYMENT_SCHEDULED";
        public static final String PAYMENT_CANCELLED = "PAYMENT_CANCELLED";
        public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
        
        private PaymentEvents() {}
    }

    // ========================================
    // NOTIFICATION EVENT TYPES
    // ========================================
    
    public static final class NotificationEvents {
        public static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
        public static final String NOTIFICATION_SENT = "NOTIFICATION_SENT";
        public static final String NOTIFICATION_FAILED = "NOTIFICATION_FAILED";
        public static final String NOTIFICATION_READ = "NOTIFICATION_READ";
        public static final String EMAIL_SENT = "EMAIL_SENT";
        public static final String SMS_SENT = "SMS_SENT";
        public static final String PUSH_NOTIFICATION_SENT = "PUSH_NOTIFICATION_SENT";
        
        private NotificationEvents() {}
    }

    // ========================================
    // FRAUD EVENT TYPES
    // ========================================
    
    public static final class FraudEvents {
        public static final String FRAUD_DETECTED = "FRAUD_DETECTED";
        public static final String FRAUD_RESOLVED = "FRAUD_RESOLVED";
        public static final String RISK_SCORE_CALCULATED = "RISK_SCORE_CALCULATED";
        public static final String HIGH_RISK_TRANSACTION = "HIGH_RISK_TRANSACTION";
        public static final String SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY";
        public static final String FRAUD_RULE_TRIGGERED = "FRAUD_RULE_TRIGGERED";
        
        private FraudEvents() {}
    }

    // ========================================
    // STATEMENT EVENT TYPES
    // ========================================
    
    public static final class StatementEvents {
        public static final String STATEMENT_GENERATED = "STATEMENT_GENERATED";
        public static final String STATEMENT_REQUESTED = "STATEMENT_REQUESTED";
        public static final String STATEMENT_SENT = "STATEMENT_SENT";
        public static final String STATEMENT_FAILED = "STATEMENT_FAILED";
        
        private StatementEvents() {}
    }

    // ========================================
    // AUDIT EVENT TYPES
    // ========================================
    
    public static final class AuditEvents {
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String ACCOUNT_ACCESS = "ACCOUNT_ACCESS";
        public static final String TRANSACTION_VIEW = "TRANSACTION_VIEW";
        public static final String CONFIGURATION_CHANGED = "CONFIGURATION_CHANGED";
        public static final String SECURITY_EVENT = "SECURITY_EVENT";
        public static final String DATA_EXPORT = "DATA_EXPORT";
        public static final String ADMIN_ACTION = "ADMIN_ACTION";
        
        private AuditEvents() {}
    }

    // ========================================
    // DLQ EVENT TYPES
    // ========================================
    
    public static final class DLQEvents {
        public static final String PROCESSING_FAILED = "PROCESSING_FAILED";
        public static final String DESERIALIZATION_ERROR = "DESERIALIZATION_ERROR";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        
        private DLQEvents() {}
    }

    // ========================================
    // RETRY EVENT TYPES
    // ========================================
    
    public static final class RetryEvents {
        public static final String RETRY_SCHEDULED = "RETRY_SCHEDULED";
        public static final String RETRY_EXHAUSTED = "RETRY_EXHAUSTED";
        
        private RetryEvents() {}
    }

    // ========================================
    // CONSUMER GROUPS
    // ========================================
    
    public static final class ConsumerGroups {
        public static final String TRANSACTION_PROCESSOR = "transaction-processor-group";
        public static final String NOTIFICATION_SERVICE = "notification-service-group";
        public static final String FRAUD_DETECTOR = "fraud-detector-group";
        public static final String AUDIT_LOGGER = "audit-logger-group";
        public static final String STATEMENT_GENERATOR = "statement-generator-group";
        public static final String ANALYTICS = "analytics-group";
        
        private ConsumerGroups() {}
    }

    // ========================================
    // PARTITION KEYS
    // ========================================
    
    public static final class PartitionKeys {
        public static final String CUSTOMER_ID = "customerId";
        public static final String ACCOUNT_ID = "accountId";
        public static final String TRANSACTION_ID = "transactionId";
        public static final String USER_ID = "userId";
        
        private PartitionKeys() {}
    }
}
