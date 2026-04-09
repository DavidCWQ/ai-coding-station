package com.cwq.project_aicodingstation.ai.guardrail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户输入护栏开关与扩展敏感词（默认词表见 {@link SafeInputGuardrail}）。
 */
@Data
@ConfigurationProperties(prefix = "ai.guardrail")
public class InputGuardrailProperties {

    /**
     * 是否对进入模型的用户消息做本地关键词校验（失败则拒绝本轮调用）。
     */
    private boolean enabled = true;

    /**
     * 额外敏感词（大小写不敏感；与内置英文词表合并）。
     */
    private List<String> sensitiveKeywords = new ArrayList<>();
}
