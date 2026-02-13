package org.example.homedatazip.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.chat.dto.ChatMessageEvent;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageEventListener {

    private final SseEmitterService sseEmitterService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageEvent(ChatMessageEvent event) {

        // 상대방이 방에 없다면
        if (!event.chatMessage().isRead()) {
            log.info("읽지 않은 메시지 개수 count={}", event.totalUnreadCount());
            sseEmitterService.sendUnreadCount(event.opponentId(), event.totalUnreadCount());
        }

        // 본인 채팅방 목록에도 최근 메시지가 갱신 되어야한다.
        sseEmitterService.sendRoomListUpdate(event.senderId());
    }
}
