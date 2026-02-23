package org.example.homedatazip.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(
        @NotBlank(message = "공지 제목은 필수!")
        String title,

        @NotBlank(message = "공지 내용은 필수!")
        String message
) {}