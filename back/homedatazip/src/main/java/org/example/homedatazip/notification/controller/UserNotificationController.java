package org.example.homedatazip.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.NotificationErrorCode;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.notification.dto.UserNotificationResponse;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.example.homedatazip.notification.service.UserNotificationService;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    private final UserRepository userRepository;

    // SSE 연결 엔드포인트
    @GetMapping(value = "/Allow-reception", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        if (!user.isNotificationEnabled()) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_DISABLED);
        }
        
        SseEmitter emitter = sseEmitterService.createEmitter(userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.TEXT_EVENT_STREAM, StandardCharsets.UTF_8));

        return ResponseEntity.ok().headers(headers).body(emitter);
    }

    // 전체 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<UserNotificationResponse>> getAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<UserNotificationResponse> responses = userNotificationService.getAllNotifications(userDetails.getUserId());

        return ResponseEntity.ok(responses);
    }

    // 읽음 알림 목록 조회
    @GetMapping("/read")
    public ResponseEntity<List<UserNotificationResponse>> getReadNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<UserNotificationResponse> responses = userNotificationService.getReadNotifications(userDetails.getUserId());

        return ResponseEntity.ok(responses);
    }

    // 미읽음 알림 목록 조회
    @GetMapping("/unread")
    public ResponseEntity<List<UserNotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<UserNotificationResponse> responses = userNotificationService.getUnreadNotifications(userDetails.getUserId());

        return ResponseEntity.ok(responses);
    }

    // 읽음 처리
    @PutMapping("/{user_notification_id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("user_notification_id") Long userNotificationId) {
        userNotificationService.markAsRead(userDetails.getUserId(), userNotificationId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
