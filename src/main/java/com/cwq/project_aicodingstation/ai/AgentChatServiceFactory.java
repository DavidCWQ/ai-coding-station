package com.cwq.project_aicodingstation.ai;

import com.cwq.project_aicodingstation.agent.service.AgentChatMessageService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 按智能体会话 id 缓存 {@link AgentChatService}（Redis 记忆 id 前缀 {@code agent_session:}，与代码生成会话 {@code session:} 隔离）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
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

    /**
     * 本地缓存 (key = 智能体会话 id)
     */
    private final Cache<Long, AgentChatService> serviceCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMinutes(30))
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .build();

    /**
     * 创建带独立记忆与历史预热的 AgentChatService
     *
     * @param sessionId 智能体会话主键
     * @return AI 服务代理
     */
    private AgentChatService createService(Long sessionId) {
        log.info("Creating AgentChatService, agentSessionId={}", sessionId);

        MessageWindowChatMemory chatMemory =
                MessageWindowChatMemory.builder()
                        .id("agent_session:" + sessionId)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(30)
                        .build();

        agentChatMessageService.loadChatHistoryToMemory(sessionId, chatMemory, 30);

        return AiServices.builder(AgentChatService.class)
                .chatModel(myChatModel)
                .streamingChatModel(myStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 获取指定智能体会话对应的 AI 服务
     *
     * @param sessionId 会话 id
     * @return 服务实例
     */
    public AgentChatService getAgentChatService(Long sessionId) {
        return serviceCache.get(sessionId, this::createService);
    }

    /**
     * 默认 Bean（占位会话 id=0，与其它 Ai Bean 用法一致）
     *
     * @return 服务实例
     */
    @Bean
    public AgentChatService agentChatService() {
        return getAgentChatService(0L);
    }
}
