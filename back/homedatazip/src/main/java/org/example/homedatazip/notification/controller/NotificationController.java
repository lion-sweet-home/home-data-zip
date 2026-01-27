package org.example.homedatazip.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.global.validation.RoleValidation;
import org.example.homedatazip.notification.dto.NotificationRequest;
import org.example.homedatazip.notification.dto.NotificationResponse;
import org.example.homedatazip.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RoleValidation roleValidation;

    // 등록
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationRequest request) {
        roleValidation.validateAdmin(userDetails.getUserId());
        NotificationResponse response = notificationService.createNotification(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        roleValidation.validateAdmin(userDetails.getUserId());
        List<NotificationResponse> responses = notificationService.getNotifications();

        return ResponseEntity.ok(responses);
    }

    // 수정
    @PutMapping("/{notification_id}")
    public ResponseEntity<NotificationResponse> updateNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("notification_id") Long notificationId,
            @Valid @RequestBody NotificationRequest request) {
        roleValidation.validateAdmin(userDetails.getUserId());
        NotificationResponse response = notificationService.updateNotification(notificationId, request);

        return ResponseEntity.ok(response);
    }

    // 삭제
    @DeleteMapping("/{notification_id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("notification_id") Long notificationId) {
        roleValidation.validateAdmin(userDetails.getUserId());
        notificationService.deleteNotification(notificationId);

        return ResponseEntity.noContent().build();
    }
}
