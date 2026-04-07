package com.cwq.project_aicodingstation.agent.config;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 智能体系统提示词加载测试（依赖 classpath 下 prompt/agent/*.txt）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
class AgentSystemPromptResolverTest {

    @Test
    void resolve_loadsAllBuiltInPrompts() {
        AgentSystemPromptResolver resolver = new AgentSystemPromptResolver();
        for (AgentCodeEnum e : AgentCodeEnum.values()) {
            String text = resolver.resolve(e);
            assertFalse(text.isBlank(), "提示词为空: " + e.getCode());
            assertTrue(text.length() > 20, "提示词过短: " + e.getCode());
        }
    }
}
