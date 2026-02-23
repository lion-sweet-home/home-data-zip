package org.example.homedatazip.global.exception.domain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApartmentErrorCode implements ErrorCode {

    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "APT_404_1", "지역 정보를 찾을 수 없습니다."),

    KEYWORD_CANNOT_BLANK(HttpStatus.BAD_REQUEST, "APT_403_1", "키워드는 공백일 수 없습니다."),
    INVALID_KEYWORD_LENGTH(HttpStatus.BAD_REQUEST, "APT_403_2", "키워드는 2글자 이상이어야 합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
