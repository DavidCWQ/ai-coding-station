package com.cwq.project_aicodingstation.common.utils;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultUtilsTest {

    @Test
    void success_wrapsDataWithSuccessCode() {
        BaseResponse<String> response = ResultUtils.success("ok-data");
        assertEquals(ErrorCode.SUCCESS.getCode(), response.getCode());
        assertEquals("ok-data", response.getData());
        assertEquals(ErrorCode.SUCCESS.getMessage(), response.getMessage());
    }

    @Test
    void error_usesGivenErrorCodeAndDefaultMessage() {
        BaseResponse<?> response = ResultUtils.error(ErrorCode.PARAMS_ERROR);
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), response.getCode());
        assertEquals(ErrorCode.PARAMS_ERROR.getMessage(), response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void error_withCustomMessage_overridesMessage() {
        BaseResponse<?> response = ResultUtils.error(ErrorCode.NO_PERMISSION, "deny");
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), response.getCode());
        assertEquals("deny", response.getMessage());
        assertNull(response.getData());
    }
}
