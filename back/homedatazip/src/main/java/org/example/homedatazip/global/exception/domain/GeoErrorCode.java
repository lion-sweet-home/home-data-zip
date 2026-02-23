package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeoErrorCode implements ErrorCode {

    // API 요청 결과가 없을 때
    RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "GEO_404_0", "API 요청에 대한 결과가 없습니다."),

    // 주소 -> 좌표 변환 중 에러
    CONVERT_ADDRESS_ERROR(HttpStatus.NOT_FOUND,"GEO_404_1","좌표 변환 중 에러가 발생했습니다."),

    // 좌표 -> 주소 변환 중 에러
    CONVERT_COORDINATE_ERROR(HttpStatus.NOT_FOUND, "GEO_404_2", "주소 변환 중 에러가 발생했습니다"),

    INVALID_ADDRESS_FORMAT(HttpStatus.BAD_REQUEST, "GEO_400_0", "주소 형식이 올바르지 않아 변환이 불가능합니다."),

    API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "GEO_429_0", "API 호출 횟수 제한을 초과했습니다."),

    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GEO_500_0", "외부 API 서버 통신 중 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
