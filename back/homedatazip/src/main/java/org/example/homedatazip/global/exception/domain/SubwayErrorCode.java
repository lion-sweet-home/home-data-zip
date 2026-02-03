package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubwayErrorCode implements ErrorCode {

    STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBWAY_404_1", "지하철 역을 찾을 수 없습니다."),
    INVALID_RADIUS(HttpStatus.BAD_REQUEST, "SUBWAY_400_1", "검색 반경은 0.5, 1, 2, 3, 5, 10(km) 중 하나여야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
