package org.example.homedatazip.ai.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.ai.dto.AiMessageRequest;
import org.example.homedatazip.ai.entity.AiMessage;
import org.example.homedatazip.ai.service.AiMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-chat")
public class AiMessageController {
    private final AiMessageService aiMessageService;

    // 질문
    @PostMapping("/ask")
    public ResponseEntity<String> ask(
            @RequestBody AiMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String answer = aiMessageService.askAndSave(userDetails.getUserId(), request.sessionId(), request.content());
        return ResponseEntity.ok(answer);
    }

    // 특정 세션 대화 내역 조회
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<List<AiMessage>> getChatMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId) {
        return ResponseEntity.ok().body(aiMessageService.getChatMessages(userDetails.getUserId(), sessionId));
    }

    // 세션 목록 조회
    @GetMapping("/sessions")
    public List<String> getSessionIds(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return aiMessageService.getSessionIds(userDetails.getUserId());
    }


}
