package com.cwq.project_aicodingstation.common.exception;

import com.cwq.project_aicodingstation.common.error.ErrorCode;

public class ForbiddenException extends BusinessException {
    public ForbiddenException() {
        super(ErrorCode.NO_PERMISSION);
    }
}
