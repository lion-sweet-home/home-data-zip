package org.example.homedatazip.chat.dto;

public record ChatRoomExitEvent(
        Long opponentId
) {
    public static ChatRoomExitEvent create(Long opponentId) {
        return new ChatRoomExitEvent(opponentId);
    }
}
