package com.cwq.project_aicodingstation.common.exception;

import com.cwq.project_aicodingstation.common.error.ErrorCode;

public class SystemException extends BaseException {

    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage());
        initCause(cause);
    }
}
