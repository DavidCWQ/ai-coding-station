package com.cwq.project_aicodingstation.common.utils;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessAssertTest {

    @Test
    void requireTrue_throwsWhenConditionFalse() {
        assertThrows(BusinessException.class,
                () -> BusinessAssert.requireTrue(false, ErrorCode.PARAMS_ERROR, "invalid"));
    }

    @Test
    void notBlank_throwsForBlankText() {
        assertThrows(BusinessException.class,
                () -> BusinessAssert.notBlank("   ", ErrorCode.PARAMS_ERROR, "blank"));
    }

    @Test
    void equals_throwsForDifferentObjects() {
        assertThrows(BusinessException.class,
                () -> BusinessAssert.equals("a", "b", ErrorCode.PARAMS_ERROR, "not equal"));
    }

    @Test
    void notEmpty_allowsCollectionWithItems() {
        assertDoesNotThrow(
                () -> BusinessAssert.notEmpty(List.of("item"), ErrorCode.PARAMS_ERROR, "empty")
        );
    }
}
