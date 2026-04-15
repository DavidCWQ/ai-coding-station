package com.cwq.project_aicodingstation.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

/**
 * 智能体多轮对话 AI 接口
 * <p>
 * 由 {@link dev.langchain4j.service.AiServices} 生成实现，配合 Redis 会话记忆。
 * </p><p>
 * 与 {@link AICodeGeneratorService} 并列：
 * </p><p>
 * - 后者固定 {@code codegen} 类 SystemMessage；
 * </p><p>
 * - 本接口用 {@code {{sys}}} 注入各智能体系统提示词。
 * </p>
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
public interface AgentChatService {

    /**
     * 流式对话（当前轮用户输入由参数传入；历史轮次由 ChatMemory 提供）
     *
     * @param systemPrompt 系统提示词（按会话所属智能体解析后的全文）
     * @param userMessage  本轮用户消息
     * @return 助手输出文本片段流
     */
    @SystemMessage("{{sys}}")
    Flux<String> chatStream(@V("sys") String systemPrompt, @UserMessage String userMessage);
}
