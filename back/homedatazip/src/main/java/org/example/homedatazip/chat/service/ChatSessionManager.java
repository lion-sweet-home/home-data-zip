package org.example.homedatazip.chat.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionManager {

    // 특정 방에 누가 있는지 확인하는 map - < roomId / email >
    private final Map<Long, Set<String>> roomParticipant = new ConcurrentHashMap<>();

    // 세션 종료 시 누구인지 추적용 map - < sessionId / (roomId,email) >
    // DISCONNECT 일때는 sessionId가 없으면 누가 어느방에서 나갔는지 알 수 없기 때문
    private final Map<String, UserSessionContext> sessionRegistry = new ConcurrentHashMap<>();

    // 세션 정보 보관을 위한 내부 record
    private record UserSessionContext(Long roomId, String email){}

    // 회원이 방에 들어왔을 때 (SUBSCRIBE)
    public void addParticipant(String sessionId, Long roomId, String email) {
        // 키가 없으면 값을 만들어서 넣고, 있으면 값 반환
        roomParticipant.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(email);
        sessionRegistry.put(sessionId, new UserSessionContext(roomId, email));
    }

    // 회원이 방에서 나갔을 때 (DISCONNECT / UNSUBSCRIBE)
    public void removeParticipant(String sessionId) {
        UserSessionContext context = sessionRegistry.remove(sessionId);
        if (context != null) { // sessionRegistry에서 성공적으로 삭제됐으면 동작
            Set<String> participants = roomParticipant.get(context.roomId());
            if (participants != null) {
                participants.remove(context.email()); // 방에서 회원 제거
                if (participants.isEmpty()) { // 방이 비었으면 정리 (두명 다 나감)
                    roomParticipant.remove(context.roomId());
                }
            }
        }
    }

    // 특정 방에 특정 유저가 있는지 확인 (읽음처리/SSE 판단)
    public boolean isUserInRoom(Long roomId, String email) {
        Set<String> participants = roomParticipant.get(roomId);
        return participants != null && participants.contains(email);
    }
}
