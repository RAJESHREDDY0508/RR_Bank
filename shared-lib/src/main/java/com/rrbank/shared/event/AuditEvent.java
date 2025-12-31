package com.rrbank.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event emitted for audit trail recording
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String eventId;
    private String eventType;
    private String userId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String ipAddress;
    private String userAgent;
    private String serviceName;
    private Map<String, Object> details;
    private String outcome; // SUCCESS, FAILURE
    private LocalDateTime timestamp;
}
