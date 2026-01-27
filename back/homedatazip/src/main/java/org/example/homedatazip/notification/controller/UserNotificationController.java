package org.example.homedatazip.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.notification.dto.UserNotificationResponse;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.example.homedatazip.notification.service.UserNotificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/notifications")
public class UserNotificationController {
    private final SseEmitterService sseEmitterService;
    private final UserNotificationService userNotificationService;

    // TODO: UserDetails로 변경 예정 (인증된 사용자의 userId 사용)
    // SSE 연결 엔드포인트
    @GetMapping(value = "/Allow-reception", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public ResponseEntity<SseEmitter> subscribe(@RequestParam Long userId) {
        SseEmitter emitter = sseEmitterService.createEmitter(userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.TEXT_EVENT_STREAM, StandardCharsets.UTF_8));
        return ResponseEntity.ok().headers(headers).body(emitter);
    }

    // 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<UserNotificationResponse>> getAllNotifications(@RequestParam Long userId) {
        List<UserNotificationResponse> responses = userNotificationService.getAllNotifications(userId);
        return ResponseEntity.ok(responses);
    }

    // 읽음 목록 조회
    @GetMapping("/read")
    public ResponseEntity<List<UserNotificationResponse>> getReadNotifications(@RequestParam Long userId) {
        List<UserNotificationResponse> responses = userNotificationService.getReadNotifications(userId);
        return ResponseEntity.ok(responses);
    }

    // 미읽음 목록 조회
    @GetMapping("/unread")
    public ResponseEntity<List<UserNotificationResponse>> getUnreadNotifications(@RequestParam Long userId) {
        List<UserNotificationResponse> responses = userNotificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(responses);
    }
}
