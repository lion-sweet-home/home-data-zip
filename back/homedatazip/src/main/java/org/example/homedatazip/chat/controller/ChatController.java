package org.example.homedatazip.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.chat.dto.ChatMessageRequest;
import org.example.homedatazip.chat.dto.ChatRoomDetailResponse;
import org.example.homedatazip.chat.dto.ChatRoomListResponse;
import org.example.homedatazip.chat.dto.ChatRoomRequest;
import org.example.homedatazip.chat.service.ChatService;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;


@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    // 채팅방 리스트 조회
    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoomListResponse>> getRooms(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();
        log.info("채팅방 리스트 조회 - userId={}", userId);
        List<ChatRoomListResponse> rooms = chatService.getRooms(userId);
        return ResponseEntity.ok(rooms);
    }

    // 채팅방 생성 or 기존 방 입장 ( 클라이언트 기준 연락하기 버튼 )
    @PostMapping("/api/chat/room")
    public ResponseEntity<?> joinOrCreateRoom(@RequestBody ChatRoomRequest request,
                                              @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info("채팅방 생성 or 방 입장 start - listingId={}", request.listingId());

        Long userId = customUserDetails.getUserId();
        // 방을 가져오거나 생성
        Long roomId = chatService.enterRoom(userId, request.listingId());

        log.info("채팅방 생성 or 방 입장 success - roomId={}", roomId);

        return ResponseEntity.ok(Collections.singletonMap("roomId", roomId));
    }

    // 특정 방의 메시지 조회
    @GetMapping("/api/chat/room/{roomId}")
    public ResponseEntity<ChatRoomDetailResponse> getMessages(@PathVariable Long roomId,
                                                         @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                         @PageableDefault(
                                                                 size = 20,
                                                                 sort = "createdAt",
                                                                 direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("채팅방 조회 - roomId={}", roomId);
        Long userId = customUserDetails.getUserId();

        // 상세 정보 및 메시지 내역 반환
        ChatRoomDetailResponse response = chatService.getRoomDetail(roomId, userId, pageable);

        return ResponseEntity.ok(response);
    }

    // 채팅 메시지 전송, db에 저장
    // MessageMapping -> /pub/chat/message 로 요청했을 때 호출
    @MessageMapping("/chat/message")
    public ResponseEntity<Void> message(ChatMessageRequest request, Principal principal) {
        log.info("채팅 메시지 전송 start - req={}", request);
        String email = principal.getName();
        chatService.sendMessage(request, email);
        return ResponseEntity.noContent().build();
    }

    // 채팅방 나가기
    @PatchMapping("/api/chat/room/{roomId}/exit")
    public ResponseEntity<Void> exitChatRoom(@PathVariable Long roomId,
                                          @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();
        chatService.exitChatRoom(roomId, userId);
        log.info("채팅방 퇴장 - roomId={}, userId={}", roomId, userId);
        return ResponseEntity.noContent().build();
    }
}
