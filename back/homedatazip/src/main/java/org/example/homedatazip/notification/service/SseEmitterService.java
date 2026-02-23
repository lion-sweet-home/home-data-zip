package org.example.homedatazip.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {

    /**
     * 채널별 SSE 연결을 분리 관리한다.
     * - chat: unreadCount, roomListUpdate
     * - notification: 공지/알림(notification)
     */
    private final Map<Long, SseEmitter> chatEmitters = new ConcurrentHashMap<>();
    private final Map<Long, SseEmitter> notificationEmitters = new ConcurrentHashMap<>();

    // 채팅 채널 SSE 생성
    public SseEmitter createChatEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 타임아웃

        emitter.onCompletion(() -> {
            log.info("SSE(chat) 연결 완료: userId={}", userId);
            chatEmitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE(chat) 연결 타임아웃: userId={}", userId);
            chatEmitters.remove(userId);
        });

        emitter.onError((ex) -> {
            log.error("SSE(chat) 연결 오류: userId={}", userId, ex);
            chatEmitters.remove(userId);
        });

        chatEmitters.put(userId, emitter);
        log.info("SSE(chat) 연결 생성: userId={}", userId);

        return emitter;
    }

    // 알림 채널 SSE 생성
    public SseEmitter createNotificationEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 타임아웃

        emitter.onCompletion(() -> {
            log.info("SSE(notification) 연결 완료: userId={}", userId);
            notificationEmitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE(notification) 연결 타임아웃: userId={}", userId);
            notificationEmitters.remove(userId);
        });

        emitter.onError((ex) -> {
            log.error("SSE(notification) 연결 오류: userId={}", userId, ex);
            notificationEmitters.remove(userId);
        });

        notificationEmitters.put(userId, emitter);
        log.info("SSE(notification) 연결 생성: userId={}", userId);

        return emitter;
    }

    // 공지/알림 전송
    public void sendNotification(Long userId, Object data) {
        SseEmitter emitter = notificationEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
                log.info("알림 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("알림 전송 실패: userId={}", userId, e);
                notificationEmitters.remove(userId);
            }
        }
    }

    // 읽지 않은 메시지 카운트 전송
    public void sendUnreadCount(Long userId, long count) {
        SseEmitter emitter = chatEmitters.get(userId);
        if (emitter != null) {
            try {
                // 전체 안읽은 개수 전송
                emitter.send(SseEmitter.event()
                        .name("unreadCount")
                        .data(count));

                // 리스트 갱신 신호 전송
                emitter.send(SseEmitter.event()
                        .name("roomListUpdate")
                        .data("refresh"));

                log.info("전체 카운트 및 리스트 갱신 신호 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("전체 카운트 및 리스트 갱신 신호 전송 실패: userId={}", userId);
                chatEmitters.remove(userId);
            }
        }
    }

    // 리스트 갱신 신호만 전송
    public void sendRoomListUpdate(Long userId) {
        SseEmitter emitter = chatEmitters.get(userId);
        if (emitter != null) {
            try {
                // 리스트 갱신 신호 전송
                emitter.send(SseEmitter.event()
                        .name("roomListUpdate")
                        .data("refresh"));

                log.info("리스트 갱신 신호 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("리스트 갱신 신호 전송 실패: userId={}", userId);
                chatEmitters.remove(userId);
            }
        }
    }

    // 채팅방 갱신 신호 전송
    public void sendRoomDetailUpdate(Long userId) {
        SseEmitter emitter = chatEmitters.get(userId);
        if (emitter != null) {
            try {
                // 채팅방 갱신 신호 전송
                emitter.send(SseEmitter.event()
                        .name("roomDetailUpdate")
                        .data("refresh"));
                log.info("채팅방 갱신 신호 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("채팅방 갱신 신호 전송 실패: userId={}", userId);
                chatEmitters.remove(userId);
            }
        }
    }

    // Heartbeat: 모든 연결에 comment만 전송 (연결되어 있는지 확인)
    public void sendHeartbeatToAll() {
        sendHeartbeat(chatEmitters, "chat");
        sendHeartbeat(notificationEmitters, "notification");
    }

    private static void sendHeartbeat(Map<Long, SseEmitter> emitters, String channel) {
        Set<Long> userIds = Set.copyOf(emitters.keySet());
        for (Long userId : userIds) {
            SseEmitter emitter = emitters.get(userId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().comment(""));
                } catch (IOException e) {
                    // 이미 끊긴 연결 정리
                    emitters.remove(userId);
                    emitter.complete();
                }
            }
        }
    }

    // SSE 연결 종료 (채팅)
    public void removeChatEmitter(Long userId) {
        SseEmitter emitter = chatEmitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE(chat) 연결 종료: userId={}", userId);
        }
    }

    // SSE 연결 종료 (공지/알림)
    public void removeNotificationEmitter(Long userId) {
        SseEmitter emitter = notificationEmitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE(notification) 연결 종료: userId={}", userId);
        }
    }

    // 채팅/알림 채널을 모두 종료
    public void removeEmitter(Long userId) {
        removeChatEmitter(userId);
        removeNotificationEmitter(userId);
    }
}