package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    // ---- Toss 연동 ----
    TOSS_APPROVE_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_502_1", "토스 결제 승인에 실패했습니다."),
    TOSS_BILLING_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_502_2", "토스 정기결제(빌링키 결제)에 실패했습니다."),

    // ---- 구독/결제로그 ----
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_404_1", "구독 정보를 찾을 수 없습니다."),
    PAYMENT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_404_2", "결제 로그를 찾을 수 없습니다."),

    // ---- 요청 값 검증 ----
    INVALID_PAYMENT_REQUEST(HttpStatus.BAD_REQUEST, "PAYMENT_400_1", "결제 요청 값이 올바르지 않습니다."),
    DUPLICATE_ORDER_ID(HttpStatus.CONFLICT, "PAYMENT_409_1", "이미 처리된 주문번호(orderId)입니다."),
    DUPLICATE_PAYMENT_KEY(HttpStatus.CONFLICT, "PAYMENT_409_2", "이미 처리된 결제키(paymentKey)입니다."),

    // ---- Batch ----
    BATCH_RECURRING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_500_1", "정기결제 배치 실행에 실패했습니다."),
    BATCH_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "PAYMENT_400_2", "배치 실행 날짜(date)가 필요합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
