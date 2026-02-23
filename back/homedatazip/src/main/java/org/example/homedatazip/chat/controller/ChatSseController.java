package org.example.homedatazip.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class ChatSseController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public ResponseEntity<?> subscribeChat(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userDetails.getUserId();
        SseEmitter emitter = sseEmitterService.createChatEmitter(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.TEXT_EVENT_STREAM, StandardCharsets.UTF_8));
        return ResponseEntity.ok().headers(headers).body(emitter);
    }
}

