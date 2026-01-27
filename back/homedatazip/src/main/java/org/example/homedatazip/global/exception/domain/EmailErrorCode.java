package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmailErrorCode implements ErrorCode {

    // 인증 데이터 자체가 없거나 만료된 경우 (Redis에서 삭제됨)
    AUTH_EXPIRED_OR_NOT_FOUND(HttpStatus.BAD_REQUEST, "EMAIL_400_1", "인증 코드가 만료되었거나 존재하지 않습니다."),

    // 코드가 일치하지 않는 경우
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "EMAIL_400_2", "인증 코드가 일치하지 않습니다."),

    // 코드는 맞지만 아직 '인증 완료' 버튼을 누르지 않은 경우
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_400_3", "이메일 인증이 완료되지 않았습니다."),

    // 이메일 발송 실패 (SMTP 설정 오류 등)
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_500_1", "이메일 발송에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
