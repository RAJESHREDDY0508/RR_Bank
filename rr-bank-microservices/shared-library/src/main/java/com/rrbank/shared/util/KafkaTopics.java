package com.rrbank.shared.util;

public class KafkaTopics {
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String ACCOUNT_CREATED = "account.created";
    public static final String ACCOUNT_UPDATED = "account.updated";
    public static final String TRANSACTION_INITIATED = "transaction.initiated";
    public static final String TRANSACTION_COMPLETED = "transaction.completed";
    public static final String TRANSACTION_FAILED = "transaction.failed";
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String NOTIFICATION_EMAIL = "notification.email";
    public static final String AUDIT_LOG = "audit.log";
    public static final String FRAUD_ALERT = "fraud.alert";
}
