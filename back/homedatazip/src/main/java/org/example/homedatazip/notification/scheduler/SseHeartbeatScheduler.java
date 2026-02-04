package org.example.homedatazip.notification.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseEmitterService sseEmitterService;

    @Scheduled(fixedDelay = 30000) // 이전 실행 완료 후 30초마다
    public void sendHeartbeat() {
        sseEmitterService.sendHeartbeatToAll();
    }
}