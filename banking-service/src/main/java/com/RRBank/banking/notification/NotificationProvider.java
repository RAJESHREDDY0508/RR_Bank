package com.RRBank.banking.notification;

import java.util.Map;

/**
 * Notification Provider Interface
 * Phase 4: Abstraction for notification delivery
 */
public interface NotificationProvider {

    /**
     * Send notification
     * @param recipient Email/phone/device token
     * @param subject Subject line (for email)
     * @param message Message body
     * @param metadata Additional data (template vars, etc.)
     * @return true if sent successfully
     */
    boolean send(String recipient, String subject, String message, Map<String, Object> metadata);

    /**
     * Get provider type
     */
    ProviderType getType();

    /**
     * Check if provider is available
     */
    boolean isAvailable();

    enum ProviderType {
        EMAIL,
        SMS,
        PUSH,
        IN_APP,
        MOCK
    }
}
