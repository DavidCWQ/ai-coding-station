package com.cwq.project_aicodingstation.ai;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import com.cwq.project_aicodingstation.agent.service.AgentChatMessageService;
import com.cwq.project_aicodingstation.ai.guardrail.InputGuardrailProperties;
import com.cwq.project_aicodingstation.ai.guardrail.SafeInputGuardrail;
import com.cwq.project_aicodingstation.ai.rag.retriever.ContentRetrieverFactory;
import com.cwq.project_aicodingstation.ai.tool.AgentToolRegistry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 按智能体会话 id 缓存 {@link AgentChatService}
 * <p>
 * Redis 记忆 id 前缀 {@code agent_session:}，与代码生成会话 {@code session:} 隔离。
 * </p>
 */
@Slf4j
@Configuration
public class AgentChatServiceFactory {

    @Resource
    private ChatModel myChatModel;

    @Resource
    private StreamingChatModel myStreamingChatModel;

    @Resource
    private AgentChatMessageService agentChatMessageService;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ObjectProvider<ContentRetrieverFactory> retrieverFactoryProvider;

    @Resource
    private AgentToolRegistry toolRegistry;

    @Resource
    private SafeInputGuardrail safeInputGuardrail;

    @Resource
    private InputGuardrailProperties guardrailProperties;

    /**
     * 本地缓存 (key = 智能体会话 id + 智能体类型)
     */
    private final Cache<CacheKey, AgentChatService> serviceCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMinutes(30))
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .build();

    /**
     * 创建带独立记忆、RAG、历史预热的 AgentChatService
     *
     * @param sessionId 智能体会话主键
     * @param agentCode 智能体编码
     * @return AI 服务代理
     */
    private AgentChatService createService(Long sessionId, AgentCodeEnum agentCode) {
        log.info("Creating AgentChatService, agentSessionId={}, agentCode={}", sessionId, agentCode);

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id("agent_session:" + sessionId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(30)
                .build();
        agentChatMessageService.loadChatHistoryToMemory(sessionId, chatMemory, 30);

        AiServices<AgentChatService> aiServices = AiServices.builder(AgentChatService.class)
                .chatModel(myChatModel)
                .streamingChatModel(myStreamingChatModel)
                .chatMemory(chatMemory);
        // ContentRetriever：按智能体 corpus 过滤（同表，不同 metadata）
        ContentRetrieverFactory retrieverFactory = retrieverFactoryProvider.getIfAvailable();
        if (retrieverFactory != null && agentCode != null) {
            aiServices.contentRetriever(retrieverFactory.forAgent(agentCode));
        }
        // Tools：按智能体选择列表
        Object[] tools = toolRegistry.toolsFor(agentCode);
        if (tools.length > 0) {
            aiServices.tools(tools);
        }
        // 只要「容器里有且仅有一个该类型 Bean」-> 执行回调（注入到 AiServices）
        if (guardrailProperties.isEnabled()) {
            aiServices.inputGuardrails(safeInputGuardrail);
        }
        return aiServices.build();
    }

    /**
     * 获取指定智能体会话对应的 AI 服务
     *
     * @param sessionId 会话 id
     * @param agentCode 智能体编码（与会话一致）
     * @return 服务实例
     */
    public AgentChatService getAgentChatService(Long sessionId, String agentCode) {
        AgentCodeEnum agent = AgentCodeEnum.fromCode(agentCode);
        CacheKey key = new CacheKey(sessionId, agent == null ? null : agent.getCode());
        return serviceCache.get(key, k -> createService(k.sessionId(), AgentCodeEnum.fromCode(k.agentCode())));
    }

    /**
     * 默认 Bean（占位会话 id=0，与其它 Ai Bean 用法一致）
     *
     * @return 服务实例
     */
    @Bean
    public AgentChatService agentChatService() {
        return getAgentChatService(0L, AgentCodeEnum.CODE_ASSISTANT.getCode());
    }

    private record CacheKey(Long sessionId, String agentCode) {
    }
}
