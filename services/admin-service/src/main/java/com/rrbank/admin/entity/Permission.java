package com.rrbank.admin.entity;

/**
 * Banking-grade permissions for RBAC system
 */
public enum Permission {
    // Dashboard
    DASHBOARD_READ,
    
    // Customer Management
    CUSTOMER_READ,
    CUSTOMER_UPDATE_STATUS,
    CUSTOMER_KYC_UPDATE,
    CUSTOMER_NOTES_WRITE,
    
    // Account Management
    ACCOUNT_READ,
    ACCOUNT_UPDATE_STATUS,
    ACCOUNT_APPROVE_REQUESTS,
    
    // Transaction Management
    TXN_READ,
    TXN_EXPORT,
    TXN_REVERSAL_REQUEST,
    
    // Payment Management
    PAYMENT_READ,
    PAYMENT_MANAGE,
    
    // Fraud Management
    FRAUD_ALERT_READ,
    FRAUD_ALERT_MANAGE,
    FRAUD_RULES_MANAGE,
    
    // Statement Management
    STATEMENT_READ,
    STATEMENT_GENERATE,
    
    // Audit
    AUDIT_READ,
    
    // Admin User Management
    ADMIN_USER_READ,
    ADMIN_USER_MANAGE,
    
    // Settings
    SETTINGS_READ,
    SETTINGS_MANAGE,
    
    // RBAC Management
    RBAC_MANAGE;
    
    public static Permission fromString(String value) {
        try {
            return Permission.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
