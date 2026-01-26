package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 회원가입 관련
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_409_1", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER_409_2", "이미 사용 중인 닉네임입니다."),

    // 역할/인증 관련
    ROLE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "USER_500_1", "기본 권한 설정을 찾을 수 없습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "USER_400_1", "이메일 인증이 완료되지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
