package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {

    private UUID id;
    private UUID userId;
    private String notificationType;
    private String channel;
    private String title;
    private String message;
    private String status;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
    private String failureReason;
    private Integer retryCount;
    private String referenceId;
    private String referenceType;
    private LocalDateTime createdAt;

    public static NotificationResponseDto fromEntity(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .notificationType(notification.getNotificationType().name())
                .channel(notification.getChannel().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus().name())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .sentAt(notification.getSentAt())
                .failureReason(notification.getFailureReason())
                .retryCount(notification.getRetryCount())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
