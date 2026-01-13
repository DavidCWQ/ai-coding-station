package com.cwq.project_aicodingstation.common.utils;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;

import java.util.Collection;
import java.util.Objects;

/**
 * Assertion Tools for Business
 * Usage: if (...) throw new BusinessException(...)
 */
public final class BusinessAssert {

    private BusinessAssert() {
        // 禁止实例化
    }

    /* ================== boolean ================== */

    /**
     * 条件为 true 则抛异常
     */
    public static void failIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }

    public static void failIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 条件为 false 则抛异常
     */
    public static void requireTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }

    public static void requireTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    /* ================== null check ================== */

    public static void notNull(Object obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new BusinessException(errorCode, message);
        }
    }

    public static void isNull(Object obj, ErrorCode errorCode, String message) {
        if (obj != null) {
            throw new BusinessException(errorCode, message);
        }
    }

    /* ================== string ================== */

    public static void notBlank(String str, ErrorCode errorCode, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(errorCode, message);
        }
    }

    public static void isBlank(String str, ErrorCode errorCode, String message) {
        if (str != null && !str.trim().isEmpty()) {
            throw new BusinessException(errorCode, message);
        }
    }

    /* ================== collection ================== */

    public static void notEmpty(Collection<?> collection, ErrorCode errorCode, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(errorCode, message);
        }
    }

    public static void isEmpty(Collection<?> collection, ErrorCode errorCode, String message) {
        if (collection != null && !collection.isEmpty()) {
            throw new BusinessException(errorCode, message);
        }
    }

    /* ================== equals ================== */

    public static void equals(Object o1, Object o2, ErrorCode errorCode, String message) {
        if (!Objects.equals(o1, o2)) {
            throw new BusinessException(errorCode, message);
        }
    }

    public static void notEquals(Object o1, Object o2, ErrorCode errorCode, String message) {
        if (Objects.equals(o1, o2)) {
            throw new BusinessException(errorCode, message);
        }
    }
}
