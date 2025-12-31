package com.RRBank.banking.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mock Notification Provider
 * Phase 4: Development/testing implementation that logs notifications
 */
@Component
@Slf4j
public class MockNotificationProvider implements NotificationProvider {

    @Override
    public boolean send(String recipient, String subject, String message, Map<String, Object> metadata) {
        log.info("=== MOCK NOTIFICATION ===");
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        if (metadata != null && !metadata.isEmpty()) {
            log.info("Metadata: {}", metadata);
        }
        log.info("=========================");
        return true;
    }

    @Override
    public ProviderType getType() {
        return ProviderType.MOCK;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
