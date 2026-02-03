package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ListingErrorCode implements ErrorCode {

    // 400
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "LST_400_1", "status 값이 올바르지 않습니다."),
    SALE_PRICE_REQUIRED(HttpStatus.BAD_REQUEST, "LST_400_2", "매매는 salePrice가 필수이며 0보다 커야 합니다."),
    DEPOSIT_REQUIRED(HttpStatus.BAD_REQUEST, "LST_400_3", "전월세는 deposit이 필수이며 0보다 커야 합니다."),
    MONTHLY_RENT_INVALID(HttpStatus.BAD_REQUEST, "LST_400_4", "전월세 monthlyRent는 0 이상이어야 합니다. (전세=0)"),
    TRADE_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "LST_400_5", "거래유형(tradeType)은 필수입니다."),

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "LST_404_1", "사용자 정보를 찾을 수 없습니다."),
    APARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "LST_404_2", "아파트 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
