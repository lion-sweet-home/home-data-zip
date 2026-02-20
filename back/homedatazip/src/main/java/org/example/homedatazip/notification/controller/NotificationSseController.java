package org.example.homedatazip.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.NotificationErrorCode;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class NotificationSseController {

    private final SseEmitterService sseEmitterService;
    private final UserRepository userRepository;

    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public ResponseEntity<SseEmitter> subscribeNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (!user.isNotificationEnabled()) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_DISABLED);
        }

        SseEmitter emitter = sseEmitterService.createNotificationEmitter(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.TEXT_EVENT_STREAM, StandardCharsets.UTF_8));
        return ResponseEntity.ok().headers(headers).body(emitter);
    }
}

