package org.example.homedatazip.notification.dto;

import org.example.homedatazip.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse (
        Long id,
        String title,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
