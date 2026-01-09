package com.cwq.project_aicodingstation.common.error;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // ===== 成功 =====
    SUCCESS(0, "ok", ErrorType.SUCCESS),

    // ===== 参数错误 1xxxx =====
    PARAMS_ERROR(10000, "请求参数错误", ErrorType.CLIENT),
    PARAMS_MISSING(10001, "缺少必要参数", ErrorType.CLIENT),
    PARAMS_INVALID(10002, "参数格式非法", ErrorType.CLIENT),

    // ===== 用户 & 权限 2xxxx =====
    NOT_LOGIN(20000, "未登录", ErrorType.AUTH),
    NO_PERMISSION(20001, "无访问权限", ErrorType.AUTH),
    TOKEN_EXPIRED(20002, "登录已过期", ErrorType.AUTH),

    // ===== 业务错误 3xxxx =====
    BUSINESS_ERROR(30000, "业务处理失败", ErrorType.BUSINESS),
    DATA_CONFLICT(30001, "数据冲突", ErrorType.BUSINESS),

    // ===== 资源错误 4xxxx =====
    NOT_FOUND(40000, "资源不存在", ErrorType.CLIENT),

    // ===== 系统错误 5xxxx =====
    SYSTEM_ERROR(50000, "系统内部异常", ErrorType.SYSTEM),
    RPC_ERROR(50001, "远程服务调用失败", ErrorType.SYSTEM);

    private final int code;
    private final String message;
    private final ErrorType type;

    ErrorCode(int code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }
}
