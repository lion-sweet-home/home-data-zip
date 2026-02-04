package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_0", "채팅방이 존재하지 않습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT_404_1","해당 채팅이 존재하지 않습니다."),
    CHAT_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "CHAT_401_0", "채팅 서비스 이용을 위해 로그인이 필요합니다."),
    CHAT_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT_403_0", "해당 채팅방에 접근 권한이 없습니다."),
    INVALID_CHAT_ROOM_ID(HttpStatus.BAD_REQUEST, "CHAT_400_0", "유효하지 않은 채팅방 식별자입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
