package com.cwq.project_aicodingstation.common.exception;

import com.cwq.project_aicodingstation.common.error.ErrorCode;

public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
