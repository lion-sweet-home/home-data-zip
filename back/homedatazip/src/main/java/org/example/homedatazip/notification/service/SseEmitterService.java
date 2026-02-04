package org.example.homedatazip.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 사용자별 SSE 생성
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 타임아웃

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}", userId);
            emitters.remove(userId);
        });

        emitter.onError((ex) -> {
            log.error("SSE 연결 오류: userId={}", userId, ex);
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);
        log.info("SSE 연결 생성: userId={}", userId);

        return emitter;
    }

    // 알림 전송 (
    public void sendNotification(Long userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
                log.info("알림 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("알림 전송 실패: userId={}", userId, e);
                emitters.remove(userId);
            }
        }
    }

    // 읽지 않은 메시지 카운트 전송
    public void sendUnreadCount(Long userId, long count) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("unreadCount")
                        .data(count));
                log.info("카운트 전송 성공: userId={}", userId);
            } catch (IOException e) {
                log.error("카운트 전송 실패: userId={}", userId);
                emitters.remove(userId);
            }
        }
    }

    // SSE 연결 종료
    public void removeEmitter(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 연결 종료: userId={}", userId);
        }
    }
}