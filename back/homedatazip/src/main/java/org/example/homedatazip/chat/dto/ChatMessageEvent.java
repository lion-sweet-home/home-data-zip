package org.example.homedatazip.chat.dto;

import lombok.AccessLevel;
import lombok.Builder;
import org.example.homedatazip.chat.entity.ChatMessage;

@Builder(access = AccessLevel.PRIVATE)
public record ChatMessageEvent(
        ChatMessage chatMessage,
        Long opponentId,
        Long senderId,
        long totalUnreadCount
) {
    public static ChatMessageEvent create(ChatMessage chatMessage, Long opponentId,
                                          Long senderId, long totalUnreadCount) {
        return ChatMessageEvent.builder()
                .chatMessage(chatMessage)
                .opponentId(opponentId)
                .senderId(senderId)
                .totalUnreadCount(totalUnreadCount)
                .build();
    }
}
