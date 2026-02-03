package org.example.homedatazip.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.chat.dto.ChatMessageRequest;
import org.example.homedatazip.chat.entity.ChatMessage;
import org.example.homedatazip.chat.entity.ChatRoom;
import org.example.homedatazip.chat.repository.ChatMessageRepository;
import org.example.homedatazip.chat.repository.ChatRoomRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    // 채팅방 생성 or 조회
    public ChatRoom getOrCreateRoom(Long buyerId, Long listingId) {
        return chatRoomRepository.findByBuyerIdAndListingId(buyerId, listingId)
                .orElseGet(() -> {
                    User buyer = userRepository.findById(buyerId)
                            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
                    Listing listing = listingRepository.findById(listingId)
                            .orElseThrow(() -> new RuntimeException("매물이 존재하지 않습니다."));// todo: 나중에 에러코드로 변경 예정
                    ChatRoom chatRoom = ChatRoom.create(buyer, listing);
                    return chatRoomRepository.save(chatRoom);
                });
    }

    // 메시지 저장
    public ChatMessage saveMessage(ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.roomId())
                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));// todo: 나중에 에러코드로 변경 예정
        User sender = userRepository.findById(request.senderId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, request.content());

        return chatMessageRepository.save(chatMessage);
    }

}
