package org.example.homedatazip.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        Long roomId,
        Long senderId,
        @NotBlank(message = "내용을 입력하세요.")
        String content
) {
}
