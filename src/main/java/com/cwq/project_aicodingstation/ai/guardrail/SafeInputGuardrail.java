package com.cwq.project_aicodingstation.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 基于关键词的输入护栏，在调用大模型前拦截明显违规内容。
 * <p>
 * 实例由 Spring 管理，并通过 {@link dev.langchain4j.service.AiServices#inputGuardrails} 注册，
 * 以便读取 {@link InputGuardrailProperties} 中的扩展词表。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeInputGuardrail implements InputGuardrail {

    private static final Set<String> BASE_SENSITIVE = Set.of(
            "terrorism",
            "bombing",
            "genocide",
            "assassination",
            "massacre",
            "childporn",
            "rape",
            "suicide",
            "self-harm"
    );

    private final InputGuardrailProperties properties;

    private Set<String> sensitiveWords = BASE_SENSITIVE;

    @PostConstruct
    void mergeKeywords() {
        Set<String> merged = new HashSet<>(BASE_SENSITIVE);
        for (String w : properties.getSensitiveKeywords()) {
            if (StringUtils.hasText(w)) {
                merged.add(w.trim().toLowerCase(Locale.ROOT));
            }
        }
        this.sensitiveWords = Set.copyOf(merged);
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        if (!properties.isEnabled()) {
            return success();
        }
        String text = userMessage.singleText().toLowerCase(Locale.ROOT);
        for (String word : sensitiveWords) {
            if (text.contains(word)) {
                log.warn("Input guardrail blocked substring match: {}", word);
                return fatal("Sensitive content detected.");
            }
        }
        String[] tokens = text.split("\\W+");
        for (String token : tokens) {
            if (StringUtils.hasText(token) && sensitiveWords.contains(token)) {
                log.warn("Input guardrail blocked token match: {}", token);
                return fatal("Prohibited content detected.");
            }
        }
        return success();
    }
}
