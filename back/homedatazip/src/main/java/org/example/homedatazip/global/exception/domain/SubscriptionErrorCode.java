package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {

    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUB_404_1", "구독 정보를 찾을 수 없습니다."),
    SUBSCRIBER_NOT_FOUND(HttpStatus.NOT_FOUND, "SUB_404_2", "구독자 정보를 찾을 수 없습니다."),

    ALREADY_SUBSCRIBED(HttpStatus.BAD_REQUEST, "SUB_400_1", "이미 구독 중입니다."),
    CANNOT_REACTIVATE_EXPIRED(HttpStatus.BAD_REQUEST, "SUB_400_2", "만료된 구독은 재등록할 수 없습니다. 새로 구독을 시작하세요."),
    INVALID_SUBSCRIPTION_STATUS(HttpStatus.BAD_REQUEST, "SUB_400_3", "구독 상태가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
