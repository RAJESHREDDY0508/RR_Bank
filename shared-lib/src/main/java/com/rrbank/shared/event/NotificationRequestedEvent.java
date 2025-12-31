package com.rrbank.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event emitted when a notification should be sent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestedEvent {
    private String eventId;
    private String userId;
    private String notificationType; // TRANSACTION_COMPLETED, FRAUD_ALERT, STATEMENT_READY
    private String title;
    private String message;
    private String channel; // IN_APP, EMAIL, PUSH
    private String referenceId;
    private String referenceType;
    private LocalDateTime timestamp;
}
