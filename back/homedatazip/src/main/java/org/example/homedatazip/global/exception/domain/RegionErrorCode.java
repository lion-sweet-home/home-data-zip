package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RegionErrorCode implements ErrorCode {

    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION_404_0", "지역을 찾을 수 없습니다."),
    INVALID_REGION_DATA(HttpStatus.BAD_REQUEST, "REGION_400_0", "필수 지역 정보(시도/구군)가 누락되었습니다."),
    DUPLICATE_REGION_CODE(HttpStatus.CONFLICT, "REGION_409_0", "중복된 지역 코드가 존재합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
