package org.example.homedatazip.chat.dto;

import jakarta.validation.constraints.NotBlank;
import org.example.homedatazip.chat.entity.MessageType;

public record ChatMessageRequest(
        MessageType type,
        Long roomId,
        @NotBlank(message = "내용을 입력하세요.")
        String content
) {
}
