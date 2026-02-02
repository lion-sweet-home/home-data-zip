package org.example.homedatazip.global.exception;

import lombok.Getter;
import org.example.homedatazip.global.exception.common.ErrorCode;

@Getter
public class BatchRetryException extends RuntimeException {

    private final ErrorCode errorCode;

    public BatchRetryException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
