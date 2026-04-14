package com.cwq.project_aicodingstation.common.exception;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SystemExceptionTest {

    @Test
    void constructorWithCause_keepsErrorCodeAndCause() {
        RuntimeException cause = new RuntimeException("boom");
        SystemException ex = new SystemException(ErrorCode.SYSTEM_ERROR, cause);

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), ex.getCode());
        assertEquals(ErrorCode.SYSTEM_ERROR.getMessage(), ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
