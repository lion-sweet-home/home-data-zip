package org.example.homedatazip.global.exception.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FavoriteErrorCode implements ErrorCode {

    LISTING_NOT_FOUND(HttpStatus.NOT_FOUND, "FAVORITE_404_1", "매물을 찾을 수 없습니다."),
    FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "FAVORITE_409_1", "이미 관심 매물로 등록된 매물입니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "FAVORITE_404_2", "관심 매물로 등록되지 않은 매물입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
