package org.example.homedatazip.chat.handler;

import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        // 우리가 던진 비즈니스 에러라면 커스텀 에러를 만들어서 반환해줘야한다.
        if (ex.getCause() instanceof BusinessException businessException) {
            return prepareErrorMessage(businessException.getErrorCode());
        }
        // 우리가 처리하지 않은 예외는 여기로
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> prepareErrorMessage(ErrorCode errorCode) {
        // 클라이언트에게 보낼 에러 메시지 프레임 생성
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);

        accessor.setMessage(errorCode.getMessage());
        accessor.setLeaveMutable(true); // 헤더를 수정 가능 상태로 설정

        // 클라이언트 분기 처리용 에러코드 전달
        accessor.setNativeHeader("code", errorCode.getCode());

        return MessageBuilder.createMessage(
                errorCode.getMessage().getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }
}
