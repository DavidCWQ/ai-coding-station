package com.cwq.project_aicodingstation.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisChatMemoryConfig {

    @Resource
    private MyRedisProperties myRedisProperties;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(myRedisProperties.getHost())
                .port(myRedisProperties.getPort())
                .user(myRedisProperties.getUsername())
                .password(myRedisProperties.getPassword())
                .ttl(myRedisProperties.getTtl())
                .build();
    }
}