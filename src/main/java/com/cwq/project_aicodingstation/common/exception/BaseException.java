package com.cwq.project_aicodingstation.common.exception;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.error.ErrorType;
import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final int code;
    private final ErrorType type;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.type = errorCode.getType();
    }

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.type = errorCode.getType();
    }
}

