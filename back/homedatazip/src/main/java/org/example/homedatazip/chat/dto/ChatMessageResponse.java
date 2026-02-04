package org.example.homedatazip.chat.dto;

import lombok.AccessLevel;
import lombok.Builder;
import org.example.homedatazip.chat.entity.ChatMessage;
import org.example.homedatazip.chat.entity.MessageType;

import java.time.LocalDateTime;

@Builder(access = AccessLevel.PRIVATE)
public record ChatMessageResponse(
        Long messageId,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt,
        MessageType type,
        boolean isRead
) {
    public static ChatMessageResponse create(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId()) // n+1 위험
                .senderNickname(message.getSender().getNickname()) // n+1 위험
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .type(message.getType())
                .isRead(message.isRead())
                .build();
    }
}
