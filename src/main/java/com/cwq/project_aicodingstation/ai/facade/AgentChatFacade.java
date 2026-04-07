package com.cwq.project_aicodingstation.ai.facade;

import com.cwq.project_aicodingstation.ai.AgentChatService;
import com.cwq.project_aicodingstation.ai.AgentChatServiceFactory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 智能体流式对话 AI 门面
 * <p>
 * 对外统一通过会话 id 获取 {@link AgentChatService} 并发起流式调用。
 * </p><p>
 * 与 {@link AICodeGeneratorFacade} 并列，分别面向「智能体多轮对话」与「应用代码生成」。
 * </p>
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Service
public class AgentChatFacade {

    @Resource
    private AgentChatServiceFactory agentChatServiceFactory;

    /**
     * 按会话发起流式对话（历史由 Factory 内记忆 + DB 预热提供）
     *
     * @param sessionId     智能体会话 id
     * @param systemPrompt  系统提示词
     * @param userMessage   本轮用户消息
     * @return 助手输出片段流
     */
    public Flux<String> chatStream(Long sessionId, String systemPrompt, String userMessage) {
        AgentChatService service = agentChatServiceFactory.getAgentChatService(sessionId);
        return service.chatStream(systemPrompt, userMessage);
    }
}
