package org.example.homedatazip.notification.dto;

import org.example.homedatazip.notification.entity.UserNotification;

import java.time.LocalDateTime;

public record UserNotificationResponse(
        Long id,
        Long notificationId,
        String title,
        String message,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static UserNotificationResponse from(UserNotification userNotification) {
        return new UserNotificationResponse(
                userNotification.getId(),
                userNotification.getNotification().getId(),
                userNotification.getNotification().getTitle(),
                userNotification.getNotification().getMessage(),
                userNotification.getCreatedAt(),
                userNotification.getReadAt()
        );
    }
}