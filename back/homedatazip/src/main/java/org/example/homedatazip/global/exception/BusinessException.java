package org.example.homedatazip.global.exception;

import lombok.Getter;
import org.example.homedatazip.global.exception.common.ErrorCode;

@Getter
public class BusinessException extends RuntimeException{

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
