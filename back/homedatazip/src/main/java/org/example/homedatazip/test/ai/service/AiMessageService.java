package org.example.homedatazip.test.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.test.ai.entity.AiMessage;
import org.example.homedatazip.test.ai.repository.AiMessageRepository;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageService {

    private final AiMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final ChatClient chatClient;
//    private final WebClient webClient;


    // 질문을 DB에 저장하고, GPT 답변까지 받아와서 다시 저장하는 로직
    public String askAndSave(Long userId, String sessionId, String userContent) {
        User user = userRepository.findById(userId).orElseThrow();

        // 사용자 메시지 저장
        AiMessage userMsg = AiMessage.createMessage(user, sessionId, userContent, "user");

        aiMessageRepository.save(userMsg);

        // DB에서 이전 대화를 가져오기
        List<AiMessage> history = aiMessageRepository.findByUserIdAndSessionIdOrderByIdAsc(userId, sessionId);

        // API 규격에 맞게 변환
       /* List<Map<String, String>> messages = history.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", messages
        );*/

        // WebClientConfig를 사용하여 실제 전송
        /*Map<String, Object> response = webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(errorBody ->
                                        Mono.error(
                                                new RuntimeException("GPT API Error: " + errorBody)
                                        )
                                )
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();*/

        // [응답에서 GPT 답변 추출]
        /*List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String gptContent = (String) message.get("content");*/

        // 위에 3 가지 과정을 Spring AI 를 이용하면 한 번에 가능
        String aiContent = chatClient.prompt()
                .messages(history.stream()
                        .map(m -> m.getRole().equals("user")
                                ? new UserMessage(m.getContent())
                                : new AssistantMessage(m.getContent()))
                        .collect(Collectors.toList()))
                .call()
                .content();

        // [GPT 답변 저장]
        AiMessage aiMsg = AiMessage.createMessage(user, sessionId, aiContent,"assistant");
        aiMessageRepository.save(aiMsg);

        return aiContent;
    }


    public List<AiMessage> getChatMessages(Long userId, String sessionId) {
        return aiMessageRepository.findByUserIdAndSessionIdOrderByIdAsc(userId, sessionId);
    }

    public List<String> getSessionIds(Long userId) {
        return aiMessageRepository.findDistinctSessionIdsByUserId(userId);
    }
}

