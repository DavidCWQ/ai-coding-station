package com.cwq.project_aicodingstation.agent.enums;

import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
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
     * 财税助理：政策口径与计算思路说明（非执业税务/会计意见）。
     */
    TAX_ASSISTANT("tax_assistant", "财税助理"),

    /**
     * 问道先生：疑惑解答与启思明理（非医疗诊断/危机热线替代）。
     */
    LIFE_ADVISOR("life_advisor", "问道先生"),

    /**
     * 灵感回声：基于管理员私有语料库的联想与对话（仅管理员账号可用）。
     */
    INSPIRATION_ECHO("inspiration_echo", "灵感回声");

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

    /**
     * 是否为仅管理员可调用的内置智能体。
     */
    public boolean isAdminOnly() {
        return this == INSPIRATION_ECHO;
    }

    /**
     * 校验当前登录用户是否允许使用该智能体（管理员专属智能体会校验角色）。
     *
     * @param agent  智能体（可为 null，此时不校验）
     * @param userVO 登录用户
     */
    public static void requireMayUse(AgentCodeEnum agent, UserLoginVO userVO) {
        if (agent == null || !agent.isAdminOnly()) {
            return;
        }
        BusinessAssert.notNull(userVO, ErrorCode.NOT_LOGIN, "用户未登录");
        BusinessAssert.equals(UserConstant.ADMIN_ROLE, userVO.getUserRole(),
                ErrorCode.NO_PERMISSION, "仅管理员可使用该智能体");
    }
}
