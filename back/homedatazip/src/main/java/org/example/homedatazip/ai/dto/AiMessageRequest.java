package org.example.homedatazip.ai.dto;

public record AiMessageRequest(
        String sessionId,
        String content
) {
}
