package org.example.homedatazip.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.notification.dto.UnreadCountResponse;
import org.example.homedatazip.notification.dto.UserNotificationResponse;
import org.example.homedatazip.notification.service.UserNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/notifications")
public class UserNotificationController {
    private final UserNotificationService userNotificationService;

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

    // 안 읽은 공지 개수
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UnreadCountResponse response = userNotificationService.getUnreadCount(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    // 읽음 처리
    @PutMapping("/{user_notification_id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("user_notification_id") Long userNotificationId) {
        userNotificationService.markAsRead(userDetails.getUserId(), userNotificationId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 전체 읽음 처리
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userNotificationService.markAllAsRead(userDetails.getUserId());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 해당 알림 삭제
    @DeleteMapping("/{user_notification_id}")
    public ResponseEntity<Void> deleteUserNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("user_notification_id") Long userNotificationId) {
        userNotificationService.deleteUserNotification(userDetails.getUserId(), userNotificationId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 읽은 알림 전체 삭제
    @DeleteMapping("/read-all")
    public ResponseEntity<Void> deleteAllReadNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userNotificationService.deleteAllReadNotifications(userDetails.getUserId());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
