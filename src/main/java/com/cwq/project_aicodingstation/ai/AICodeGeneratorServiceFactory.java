package com.cwq.project_aicodingstation.ai;

import com.cwq.project_aicodingstation.chat.service.ChatHistoryService;
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

@Slf4j
@Configuration
public class AICodeGeneratorServiceFactory {

    @Resource
    private ChatModel myChatModel;

    @Resource
    private StreamingChatModel myStreamingChatModel;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 本地缓存 (key = sessionId)
     */
    private final Cache<Long, AICodeGeneratorService> serviceCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMinutes(30))
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .build();

    /**
     * 创建 Service Core
     */
    private AICodeGeneratorService createService(Long sessionId) {

        log.info("Creating AI Service, sessionId={}", sessionId);

        // 根据 sessionId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory =
                MessageWindowChatMemory.builder()
                        .id("session:" + sessionId) // Based on sessionId
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build();

        // 从 DB 加载历史对话到记忆中（按 sessionId）
        chatHistoryService.loadChatHistoryToMemory(sessionId, chatMemory, 20);

        return AiServices.builder(AICodeGeneratorService.class)
                .chatModel(myChatModel)
                .streamingChatModel(myStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 获取 AI Service (基于 sessionId)
     * <p>
     * 数据加载肯定是耗时操作，所以我们引入了本地缓存（含金量++）
     * </p>
     */
    public AICodeGeneratorService getAICodeGeneratorService(Long sessionId) {
        return serviceCache.get(sessionId, this::createService);
    }

    @Bean // 默认提供一个 Bean
    public AICodeGeneratorService aiCodeGeneratorService() {
        return getAICodeGeneratorService(0L);
    }

}
