package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {

    // 404
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUB_404_1", "구독 정보를 찾을 수 없습니다."),
    SUBSCRIBER_NOT_FOUND(HttpStatus.NOT_FOUND, "SUB_404_2", "구독자 정보를 찾을 수 없습니다."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "SUB_404_3", "필요한 권한(Role)을 찾을 수 없습니다."),

    // 400 - 상태/정책
    ALREADY_SUBSCRIBED(HttpStatus.BAD_REQUEST, "SUB_400_1", "이미 구독 중입니다."),
    CANNOT_REACTIVATE_EXPIRED(HttpStatus.BAD_REQUEST, "SUB_400_2", "만료된 구독은 재등록할 수 없습니다. 새로 구독을 시작하세요."),
    INVALID_SUBSCRIPTION_STATUS(HttpStatus.BAD_REQUEST, "SUB_400_3", "구독 상태가 올바르지 않습니다."),
    BATCH_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "SUB_400_4", "배치 실행 날짜(date)가 필요합니다."),

    // 400 - billing / customer
    INVALID_CUSTOMER_KEY(HttpStatus.BAD_REQUEST, "SUB_400_5", "customerKey가 올바르지 않습니다."),
    BILLING_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "SUB_400_6", "billingKey가 필요합니다."),

    // 추가: 카드 등록 안된 상태에서 '구독 시작(첫 결제)' 시도
    BILLING_AUTH_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "SUB_400_7", "authKey가 필요합니다."),
    BILLING_KEY_ISSUE_FAILED(HttpStatus.BAD_REQUEST, "SUB_400_8", "billingKey 발급에 실패했습니다."),
    BILLING_KEY_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "SUB_400_9", "카드 등록이 필요합니다. billingKey가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
