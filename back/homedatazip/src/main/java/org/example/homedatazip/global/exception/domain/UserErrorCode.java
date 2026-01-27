package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    ADMIN_ONLY(HttpStatus.FORBIDDEN, "USER_403_01", "관리자만 접근할 수 있습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_01", "사용자를 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
