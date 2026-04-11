package com.cwq.project_aicodingstation.agent.enums;

import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import lombok.Getter;

/**
 * 平台内置智能体编码（与库表 agent_code、前端路由参数一致）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Getter
public enum AgentCodeEnum {

    /**
     * 编程助手：代码解释、基础知识、面试场景（不替代实际运行/测试）。
     */
    CODE_ASSISTANT("code_assistant", "编程助手"),

    /**
     * 财税助手：政策口径与计算思路说明（非执业税务/会计意见）。
     */
    TAX_ASSISTANT("tax_assistant", "财税助手"),

    /**
     * 哲学顾问：心理支持与哲学思辨（非医疗诊断/危机热线替代）。
     */
    LIFE_ADVISOR("life_advisor", "哲学顾问");

    /**
     * 持久化与接口使用的编码
     */
    private final String code;

    /**
     * 展示名称
     */
    private final String displayName;

    AgentCodeEnum(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 根据 code 解析枚举
     *
     * @param code 编码字符串
     * @return 枚举，无法识别时返回 null
     */
    public static AgentCodeEnum fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        String t = code.trim();
        for (AgentCodeEnum e : values()) {
            if (e.code.equals(t)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 解析并校验 code 合法性（非法时抛业务异常）。
     *
     * @param code 编码字符串
     * @return 枚举
     */
    public static AgentCodeEnum requireValid(String code) {
        AgentCodeEnum e = fromCode(code);
        BusinessAssert.notNull(e, ErrorCode.PARAMS_ERROR, "不支持的智能体编码");
        return e;
    }
}
