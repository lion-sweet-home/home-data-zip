package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    // 공지사항 관련
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_1", "공지사항을 찾을 수 없습니다."),

    // 사용자 알림 관련
    USER_NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_2", "알림을 찾을 수 없습니다."),

    // 알림 수신 설정 관련
    NOTIFICATION_DISABLED(HttpStatus.BAD_REQUEST, "NOTIFICATION_400_1", "알림 수신 설정이 비활성화되어 있습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "PAY_401_1", "인증이 필요합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
