package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    // 400
    PAYMENT_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "PAY_400_1", "paymentKey는 필수입니다."),
    ORDER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "PAY_400_2", "orderId는 필수입니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "PAY_400_3", "결제 금액이 올바르지 않습니다."),
    ALREADY_SUBSCRIBED(HttpStatus.BAD_REQUEST, "PAY_400_4", "이미 구독 중입니다."),
    BATCH_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "PAY_400_5", "배치 실행 날짜가 필요합니다."),
    INVALID_CUSTOMER_KEY(HttpStatus.BAD_REQUEST, "PAY_400_6", "customerKey 형식이 올바르지 않습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "PAY_401_1", "로그인이 필요합니다."),

    // 404
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_404_1", "구독 정보를 찾을 수 없습니다."),
    PAYMENT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_404_2", "결제 로그를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY_404_3", "사용자를 찾을 수 없습니다."),

    // 409
    DUPLICATE_ORDER_ID(HttpStatus.CONFLICT, "PAY_409_1", "이미 처리된 주문번호(orderId)입니다."),
    BATCH_ALREADY_RUNNING(HttpStatus.CONFLICT, "PAY_409_2", "배치가 이미 실행 중입니다."),

    // 502 (외부 연동 실패)
    TOSS_APPROVE_FAILED(HttpStatus.BAD_GATEWAY, "PAY_502_1", "토스 결제 승인에 실패했습니다."),
    TOSS_BILLING_FAILED(HttpStatus.BAD_GATEWAY, "PAY_502_2", "토스 정기결제에 실패했습니다."),
    TOSS_BILLING_KEY_ISSUE_FAILED(HttpStatus.BAD_GATEWAY, "PAY_502_3", "토스 빌링키 발급에 실패했습니다."),
    TOSS_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "PAY_502_4", "토스 응답 형식이 올바르지 않습니다."),

    // 500
    BATCH_RECURRING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAY_500_1", "정기결제 배치 실행에 실패했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "SUB_500_2", "단건 결제는 불가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
