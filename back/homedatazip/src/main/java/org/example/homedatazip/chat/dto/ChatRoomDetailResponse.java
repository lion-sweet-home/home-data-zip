package org.example.homedatazip.chat.dto;

import lombok.AccessLevel;
import lombok.Builder;
import org.springframework.data.domain.Slice;

@Builder(access = AccessLevel.PRIVATE)
public record ChatRoomDetailResponse(
        Long roomId,
        String listingName, // 매물명
        String opponentNickname, // 상대방( 판매자 or 구매자 ) 이름
        Slice<ChatMessageResponse> messages // 과거 메시지 내역
) {
    public static ChatRoomDetailResponse create(Long roomId, String listingName, String opponentNickname,
                                                Slice<ChatMessageResponse> messages) {
        return ChatRoomDetailResponse.builder()
                .roomId(roomId)
                .listingName(listingName)
                .opponentNickname(opponentNickname)
                .messages(messages)
                .build();
    }

}
