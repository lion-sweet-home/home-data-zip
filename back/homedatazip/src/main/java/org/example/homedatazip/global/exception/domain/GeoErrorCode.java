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
    CONVERT_COORDINATE_ERROR(HttpStatus.NOT_FOUND, "GEO_404_2", "주소 변환 중 에러가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
