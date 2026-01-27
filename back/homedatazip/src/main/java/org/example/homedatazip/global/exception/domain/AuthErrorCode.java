package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    //회원정보 없음
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "회원정보가 없습니다."),

    //회원 로그인 실패
    INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "이메일 또는 비밀번호가 올바르지 않습니다."),

    //리프레쉬토큰 만료
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "REFRESH_401_1", "리프레쉬 토큰이 없습니다."),
    INVALID_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "REFRESH_401_2", "리프레쉬 토큰 만료되었습니다.");



    private final HttpStatus status;
    private final String code;
    private final String message;
}
