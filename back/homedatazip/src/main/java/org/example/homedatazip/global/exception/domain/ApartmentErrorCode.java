package org.example.homedatazip.global.exception.domain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApartmentErrorCode implements ErrorCode {

    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "APT_404_1", "지역 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
