package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_404_1", "결제 로그를 찾을 수 없습니다."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_404_2", "구독 정보를 찾을 수 없습니다."),

    DUPLICATE_ORDER_ID(HttpStatus.BAD_REQUEST, "PAY_400_1", "이미 처리된 주문번호입니다."),
    DUPLICATE_PAYMENT_KEY(HttpStatus.BAD_REQUEST, "PAY_400_2", "이미 처리된 결제키입니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "PAY_400_3", "결제 금액이 올바르지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "PAY_400_4", "결제 상태가 올바르지 않습니다."),

    TOSS_APPROVE_FAILED(HttpStatus.BAD_GATEWAY, "PAY_502_1", "토스 결제 승인에 실패했습니다."),
    TOSS_BILLING_FAILED(HttpStatus.BAD_GATEWAY, "PAY_502_2", "토스 정기결제 요청에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
