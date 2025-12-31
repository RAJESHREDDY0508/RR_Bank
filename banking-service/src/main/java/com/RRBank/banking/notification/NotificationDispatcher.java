package com.RRBank.banking.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification Dispatcher
 * Phase 4: Routes notifications to appropriate providers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final List<NotificationProvider> providers;

    /**
     * Send notification via all available providers of specified type
     */
    public boolean sendNotification(NotificationProvider.ProviderType type, 
                                    String recipient, String subject, String message) {
        return sendNotification(type, recipient, subject, message, new HashMap<>());
    }

    /**
     * Send notification with metadata
     */
    public boolean sendNotification(NotificationProvider.ProviderType type,
                                    String recipient, String subject, String message,
                                    Map<String, Object> metadata) {
        log.info("Dispatching {} notification to {}", type, recipient);

        boolean sent = false;
        for (NotificationProvider provider : providers) {
            if (provider.getType() == type && provider.isAvailable()) {
                try {
                    sent = provider.send(recipient, subject, message, metadata) || sent;
                } catch (Exception e) {
                    log.error("Failed to send via {}: {}", provider.getType(), e.getMessage());
                }
            }
        }

        // Fallback to mock if no provider available
        if (!sent) {
            for (NotificationProvider provider : providers) {
                if (provider.getType() == NotificationProvider.ProviderType.MOCK) {
                    sent = provider.send(recipient, subject, message, metadata);
                    break;
                }
            }
        }

        return sent;
    }

    /**
     * Send email notification
     */
    public boolean sendEmail(String email, String subject, String message) {
        return sendNotification(NotificationProvider.ProviderType.EMAIL, email, subject, message);
    }

    /**
     * Send SMS notification
     */
    public boolean sendSms(String phone, String message) {
        return sendNotification(NotificationProvider.ProviderType.SMS, phone, "SMS", message);
    }

    /**
     * Send push notification
     */
    public boolean sendPush(String deviceToken, String title, String message) {
        return sendNotification(NotificationProvider.ProviderType.PUSH, deviceToken, title, message);
    }
}
