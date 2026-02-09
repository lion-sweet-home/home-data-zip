package org.example.homedatazip.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.chat.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatAuthService {

    private final ChatRoomRepository chatRoomRepository;

    // 웹소켓 구독시 해당 유저가 판매자 혹은 구매자 인지 체크
    public boolean isUserParticipant(String email, Long roomId) {
        return chatRoomRepository.existsParticipant(roomId, email);
    }
}
