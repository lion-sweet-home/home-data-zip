package org.example.homedatazip.user.dto;

public record NotificationSettingResponse(
        boolean notificationEnabled
) {
    public static NotificationSettingResponse from(boolean notificationEnabled) {
        return new NotificationSettingResponse(notificationEnabled);
    }
}

