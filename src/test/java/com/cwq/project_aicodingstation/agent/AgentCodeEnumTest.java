package com.cwq.project_aicodingstation.agent;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 智能体编码枚举单元测试。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
class AgentCodeEnumTest {

    @Test
    void fromCode_recognizesBuiltIns() {
        assertEquals(AgentCodeEnum.CODE_ASSISTANT, AgentCodeEnum.fromCode("code_assistant"));
        assertEquals(AgentCodeEnum.TAX_ASSISTANT, AgentCodeEnum.fromCode("tax_assistant"));
        assertEquals(AgentCodeEnum.LIFE_ADVISOR, AgentCodeEnum.fromCode("life_advisor"));
    }

    @Test
    void fromCode_trimsInput() {
        assertNotNull(AgentCodeEnum.fromCode("  code_assistant  "));
    }

    @Test
    void fromCode_returnsNullForUnknown() {
        assertNull(AgentCodeEnum.fromCode("unknown_bot"));
        assertNull(AgentCodeEnum.fromCode(""));
        assertNull(AgentCodeEnum.fromCode(null));
    }
}
