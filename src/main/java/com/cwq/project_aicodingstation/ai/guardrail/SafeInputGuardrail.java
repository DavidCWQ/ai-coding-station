package com.cwq.project_aicodingstation.ai.guardrail;

import com.cwq.project_aicodingstation.common.exception.BusinessException;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 智能体模型调用上的第二道检查。页面入口会先用 {@link PromptGuard} 直接拒绝，
 * 这里只挡住绕过入口、仍然走进模型代理的用户消息。
 */
@Component
@RequiredArgsConstructor
public class SafeInputGuardrail implements InputGuardrail {

    private final InputGuardrailProperties properties;
    private final PromptGuard promptGuard;

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        if (!properties.isEnabled() || userMessage == null) {
            return success();
        }
        String text = userMessage.singleText();
        try {
            promptGuard.check(text, null, "model");
        } catch (BusinessException ex) {
            return fatal(ex.getMessage());
        }
        return success();
    }
}
