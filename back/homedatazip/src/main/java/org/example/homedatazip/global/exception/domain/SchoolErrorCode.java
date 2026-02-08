package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SchoolErrorCode implements ErrorCode {

    SCHOOL_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHOOL_404_1", "학교를 찾을 수 없습니다."),
    INVALID_RADIUS(HttpStatus.BAD_REQUEST, "SCHOOL_400_1", "검색 반경은 0.5, 1, 2, 3, 5, 10(km) 중 하나여야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}