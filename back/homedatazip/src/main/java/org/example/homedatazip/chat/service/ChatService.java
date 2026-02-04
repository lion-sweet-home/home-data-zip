package org.example.homedatazip.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.chat.dto.ChatMessageRequest;
import org.example.homedatazip.chat.dto.ChatMessageResponse;
import org.example.homedatazip.chat.dto.ChatRoomDetailResponse;
import org.example.homedatazip.chat.entity.ChatMessage;
import org.example.homedatazip.chat.entity.ChatRoom;
import org.example.homedatazip.chat.entity.MessageType;
import org.example.homedatazip.chat.repository.ChatMessageRepository;
import org.example.homedatazip.chat.repository.ChatRoomRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ChatErrorCode;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatSessionManager chatSessionManager;
    private final SseEmitterService sseEmitterService;

    // 채팅방 입장
    public Long enterRoom(Long userId, Long listingId) {
        log.info("채팅방 입장 - userId={}, listingId={}", userId, listingId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.error("매물이 존재하지 않습니다. listingId={}", listingId);
                    return new RuntimeException("매물이 존재하지 않습니다.");
                });// todo: 나중에 에러코드로 변경 예정

        // user가 판매자인 경우
        if (listing.getUser().getId().equals(userId)) {
            // 판매자는 방을 직접 생성할 수 없다.
            return chatRoomRepository.findByListingId(listingId)
                    .map(ChatRoom::getId)
                    .orElseThrow(() -> {
                        log.error("채팅방이 존재하지 않습니다. userId={}, listingId={}", userId, listingId);
                        return new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                    });
        }

        // user가 구매자인 경우
        return chatRoomRepository.findByBuyerIdAndListingId(userId, listingId)
                .map(ChatRoom::getId)
                .orElseGet(() -> {
                    log.info("채팅방을 생성합니다 - userId={}, listingId={}", userId, listingId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> {
                                log.error("회원이 존재하지 않습니다. userId={}", userId);
                                return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                            });
                    ChatRoom chatRoom = ChatRoom.create(user, listing);
                    return chatRoomRepository.save(chatRoom).getId();
                });
    }

    // 방 상세 정보 + 메시지 내역 조회
    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getRoomDetail(Long roomId, Long userId, Pageable pageable) {
        log.info("방 정보 + 메시지 내역 조회 - roomId={}, userId={}", roomId, userId);
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("채팅방이 존재하지 않습니다. roomId={}", roomId);
                    return new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        // 방 들어갔을 때 안읽은 메시지 읽음 처리
        chatMessageRepository.markAsReadByRoomIdAndReceiverId(roomId, userId);

        // sse로 안읽은 메시지 개수 갱신해서 다시 보내주기
        long totalUnread = chatMessageRepository.countTotalUnreadMessages(userId);
        sseEmitterService.sendUnreadCount(userId, totalUnread);

        // 상대방 찾기 - 내가 구매자면 판매자로, 판매자면 구매자로
        User opponent = findOpponent(userId, chatRoom);

        // 메시지 내역 조회
        Slice<ChatMessageResponse> messages = chatMessageRepository.findByChatRoomId(chatRoom.getId(), pageable)
                .map(ChatMessageResponse::create);

        log.info("메시지 내역 조회 성공 - size={}", messages.getSize());

        return ChatRoomDetailResponse.create(
                roomId,
                chatRoom.getListing().getApartment().getAptName(),
                opponent.getNickname(),
                messages
        );
    }

    // 메시지 전송
    public void sendMessage(ChatMessageRequest request, String email) {
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("회원을 찾을 수 없습니다. - email={}", email);
                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });

        ChatRoom chatRoom = chatRoomRepository.findById(request.roomId())
                .orElseThrow(() -> {
                    log.error("채팅방을 찾을 수 없습니다. - roomId={}", request.roomId());
                    return new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        // 상대방 찾기
        User opponent = findOpponent(sender.getId(), chatRoom);

        // 상대방이 현재 방을 보고있는지 체크
        boolean isRead = chatSessionManager.isUserInRoom(request.roomId(), opponent.getEmail());

        // 입장 혹은 퇴장시 채팅 내용 변경
        String content = request.content();
        if (request.type() == MessageType.ENTER) {
            content = sender.getNickname() + "님이 입장하셨습니다.";
        } else if (request.type() == MessageType.LEAVE) {
            content = sender.getNickname() + "님이 퇴장하셨습니다.";
        }

        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content, request.type(), isRead);
        chatMessageRepository.save(chatMessage);

        // 상대방이 방에 없다면
        if (!isRead) {
            long totalUnread = chatMessageRepository.countTotalUnreadMessages(opponent.getId());
            log.info("읽지 않은 메시지 개수 - opponent={}, count={}", opponent.getNickname(), totalUnread);
            // sse로 숫자 전송
            sseEmitterService.sendUnreadCount(opponent.getId(), totalUnread);
        }

        ChatMessageResponse response = ChatMessageResponse.create(chatMessage);

        messagingTemplate.convertAndSend("/sub/chat/room/" + request.roomId(), response);

        log.info("메시지 전송 완료 - roomId={}", request.roomId());

    }

    // 상대방 찾기
    private static User findOpponent(Long userId, ChatRoom chatRoom) {
        User opponent = chatRoom.getBuyer().getId().equals(userId)
                ? chatRoom.getListing().getUser()
                : chatRoom.getBuyer();
        return opponent;
    }

}
