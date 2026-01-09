package com.cwq.project_aicodingstation.common.utils;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.response.BaseResponse;

public final class ResultUtils {

    private ResultUtils() {}

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.success(data);
    }

    public static BaseResponse<?> error(ErrorCode errorCode) {
        return BaseResponse.error(errorCode);
    }

    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return BaseResponse.error(errorCode.getCode(), message);
    }
}
