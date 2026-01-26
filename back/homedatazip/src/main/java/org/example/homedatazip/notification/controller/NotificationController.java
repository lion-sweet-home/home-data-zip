package org.example.homedatazip.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.notification.dto.NotificationRequest;
import org.example.homedatazip.notification.dto.NotificationResponse;
import org.example.homedatazip.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
public class NotificationController {

    // TODO: ADMIN만 가능하게 수정해야함
    // TODO: UserDetails로 변경 예정

    private final NotificationService notificationService;

    // 등록
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications() {
        List<NotificationResponse> responses = notificationService.getNotifications();
        return ResponseEntity.ok(responses);
    }

    // 수정
    @PutMapping("/{notification_id}")
    public ResponseEntity<NotificationResponse> updateNotification(
            @PathVariable("notification_id") Long notificationId,
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.updateNotification(notificationId, request);
        return ResponseEntity.ok(response);
    }

    // 삭제
    @DeleteMapping("/{notification_id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("notification_id") Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }
}
