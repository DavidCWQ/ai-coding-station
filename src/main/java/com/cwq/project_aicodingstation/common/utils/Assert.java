package com.cwq.project_aicodingstation.common.utils;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;

import java.util.Collection;
import java.util.Objects;

/**
 * Assertion Tools for Business
 * Usage: if (...) throw new BusinessException(...)
 */
public final class Assert {

    private Assert() {
        // 禁止实例化
    }

    /* ================== boolean ================== */

    /**
     * 条件为 true 则抛异常
     */
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }

    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 条件为 false 则抛异常
     */
    public static void isFalse(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }

    /* ================== null check ================== */

    public static void notNull(Object obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new BusinessException(errorCode);
        }
    }

    public static void isNull(Object obj, ErrorCode errorCode) {
        if (obj != null) {
            throw new BusinessException(errorCode);
        }
    }

    /* ================== string ================== */

    public static void notBlank(String str, ErrorCode errorCode) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    public static void isBlank(String str, ErrorCode errorCode) {
        if (str != null && !str.trim().isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    /* ================== collection ================== */

    public static void notEmpty(Collection<?> collection, ErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    public static void isEmpty(Collection<?> collection, ErrorCode errorCode) {
        if (collection != null && !collection.isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    /* ================== equals ================== */

    public static void equals(Object o1, Object o2, ErrorCode errorCode) {
        if (!Objects.equals(o1, o2)) {
            throw new BusinessException(errorCode);
        }
    }

    public static void notEquals(Object o1, Object o2, ErrorCode errorCode) {
        if (Objects.equals(o1, o2)) {
            throw new BusinessException(errorCode);
        }
    }
}
