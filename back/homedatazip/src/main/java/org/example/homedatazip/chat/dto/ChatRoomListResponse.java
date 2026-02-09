package org.example.homedatazip.chat.dto;

import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(access = AccessLevel.PRIVATE)
public record ChatRoomListResponse(
        Long roomId,
        String listingName, // 매물명
        String lastMessage, // 최근에 온 메시지
        LocalDateTime lastMessageTime, // 최근 메시지 온 시간
        Long unReadCount // 읽지 않은 메시지 개수
) {
    public static ChatRoomListResponse create(Long roomId, String listingName, String lastMessage,
                                              LocalDateTime lastMessageTime, Long unReadCount) {
        return ChatRoomListResponse.builder()
                .roomId(roomId)
                .listingName(listingName)
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .unReadCount(unReadCount)
                .build();
    }
}
