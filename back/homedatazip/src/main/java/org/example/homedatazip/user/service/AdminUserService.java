package org.example.homedatazip.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.chat.entity.ChatRoom;
import org.example.homedatazip.chat.repository.ChatRoomRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.user.dto.UserSearchRequest;
import org.example.homedatazip.user.dto.UserSearchResponse;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchUsers(UserSearchRequest request, Pageable pageable) {

        Page<User> users;
        switch (request.type()) {
            case "ALL" -> users = userRepository.searchAll(request.keyword(), pageable);
            case "NICKNAME" -> users = userRepository.searchByNickname(request.keyword(), pageable);
            case "EMAIL" -> users = userRepository.searchByEmail(request.keyword(), pageable);
            default -> {
                log.error("검색 타입이 올바르지 않습니다. type={}", request.type());
                throw new BusinessException(UserErrorCode.INVALID_SEARCH_TYPE);
            }
        }
        return users.map(UserSearchResponse::create);
    }

    public void deleteUser(Long userId) {
        log.info("{}",userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("회원을 찾을 수 없습니다. userId={}", userId);
                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });

        log.info("{}",user.getEmail());


        // 유저가 참여 중인 모든 채팅방에서 퇴장 처리
        for (ChatRoom chatRoom : chatRoomRepository.findAllByUserId(userId)) {
            if (chatRoom.isBuyer(userId)) {
                chatRoom.exitBuyer();
            } else {
                chatRoom.exitSeller();
            }

            // 둘 다 나갔다면 채팅방 삭제
            if (chatRoom.isBuyerExited() && chatRoom.isSellerExited()) {
                chatRoomRepository.delete(chatRoom);
            }
        }

        // 유저 삭제 - 매물, 관심매물, 토큰도 같이 삭제됨.
        userRepository.delete(user);
    }
}
