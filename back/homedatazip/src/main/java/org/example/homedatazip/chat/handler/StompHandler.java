package org.example.homedatazip.chat.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.chat.service.ChatAuthService;
import org.example.homedatazip.chat.service.ChatSessionManager;
import org.example.homedatazip.global.config.CustomUserDetailsService;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ChatErrorCode;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.global.jwt.util.JwtTokenizer;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenizer jwtTokenizer;
    private final CustomUserDetailsService customUserDetailsService;
    private final ChatSessionManager chatSessionManager;
    private final ChatAuthService chatAuthService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // 순환참조 방지를 위해 생성자에 @Lazy를 붙여 진짜 사용할때 주입받는 방식으로 변경.
    public StompHandler(JwtTokenizer jwtTokenizer,
                        CustomUserDetailsService customUserDetailsService,
                        ChatSessionManager chatSessionManager,
                        ChatAuthService chatAuthService,
                        @Lazy SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.jwtTokenizer = jwtTokenizer;
        this.customUserDetailsService = customUserDetailsService;
        this.chatSessionManager = chatSessionManager;
        this.chatAuthService = chatAuthService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        log.info("==========웹소켓 요청 시도 - command={}, destination={}==========", accessor.getCommand(), accessor.getDestination());

        // 웹소켓 연결 요청 시
        if (StompCommand.CONNECT == accessor.getCommand()) {
            // 헤더에서 토큰 추출
            String accessToken = accessor.getFirstNativeHeader("Authorization");
            log.info("웹 소켓 연결 요청 - accessToken={}", accessToken);

            // 토큰 누락
            if (accessToken == null) {
                throw new BusinessException(ChatErrorCode.CHAT_UNAUTHORIZED);
            }

            // Bearer 붙어있다면 자르기
            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
            try {
                // 토큰 검증 및 유저 정보 추출
                if (jwtTokenizer.validateAccessToken(accessToken)) {
                    // 토큰에서 유저 식별자 추출
                    String email = jwtTokenizer.getEmailFromAccessToken(accessToken);

                    log.info("토큰에서 유저 식별자 추출 - email={}", email);

                    // 유저 정보 로드
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                    // 인증 객체 생성
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    log.info("인증 객체 생성 - authentication={}", authentication);

                    // 웹소켓 세션에 유저 저장(@MessageMapping에서 Principal로 유저 가져올 수 있음)
                    accessor.setUser(authentication);

                    log.info("인증 객체 세션 주입 완료 - {}", authentication.getName());
                } else {
                    throw new BusinessException(ChatErrorCode.CHAT_UNAUTHORIZED);
                }
            } catch (Exception e) {
                throw new BusinessException(ChatErrorCode.CHAT_UNAUTHORIZED);
            }

        }

        // 구독시 권한 검증
        if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            String email = getEmail(accessor);
            String destination = accessor.getDestination(); // ex: /sub/chat/room/1
            if (destination != null && destination.startsWith("/sub/chat/room/")) {
                Long roomId = getRoomId(destination);
                log.info("웹소켓 구독 시 권한 검증 - roomId={}, email={}", roomId, email);
                // 구독시 해당 유저가 판매자 혹은 구매자 인지 체크
                if (!chatAuthService.isUserParticipant(email, roomId)) {
                    log.error("접근 권한이 없습니다. email={}, roomId={}", email, roomId);
                    throw new BusinessException(ChatErrorCode.CHAT_FORBIDDEN);
                }
            }
        }

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();

        log.info("PostSend - sessionId={}, command={}", sessionId, accessor.getCommand());

        try {
            if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
                Long roomId = getRoomId(accessor.getDestination());
                String email = getEmail(accessor);
                log.info("웹 소켓 구독 요청 - roomId={}, email={}", roomId, email);
                chatSessionManager.addParticipant(sessionId, roomId, email);

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

                // 읽음 신호 발행 - 내가 방에 들어갔을 때 상대방이 자신의 채팅 옆에 1이 사라질수 있도록 알려준다.
                messagingTemplate.convertAndSend("/sub/chat/room/" + roomId,
                        Map.of("type", "READ_ALL", "readerNickname", user.getNickname()));

            } else if (StompCommand.DISCONNECT == accessor.getCommand()) {
                chatSessionManager.removeParticipant(sessionId);
            }
        } catch (Exception e) {
            log.error("SUBSCRIBE 요청 단계에서 에러 발생 - error={}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static String getEmail(StompHeaderAccessor accessor) {
        // User 객체가 있는지 확인
        Authentication authentication = (Authentication) accessor.getUser();
        if (authentication == null) {
            log.error("세션에 인증정보가 없습니다.");
            throw new BusinessException(ChatErrorCode.CHAT_UNAUTHORIZED);
        }
        // email 추출
        String email;
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            log.error("인증 객체의 타입이 유효하지 않습니다. - {}", authentication.getPrincipal().getClass());
            throw new BusinessException(ChatErrorCode.CHAT_UNAUTHORIZED);
        }
        return email;
    }

    // destination : /sub/chat/room/1 -> 여기서 1만 반환해주는 메서드
    private static Long getRoomId(String destination) {
        try {
            return Long.parseLong(destination.substring(destination.lastIndexOf("/") + 1));
        } catch (Exception e) {
            log.error("Room Id 파싱 실패 - {}", destination);
            throw new BusinessException(ChatErrorCode.INVALID_CHAT_ROOM_ID);
        }
    }
}
