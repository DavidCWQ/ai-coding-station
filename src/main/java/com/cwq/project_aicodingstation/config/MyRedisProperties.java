package com.cwq.project_aicodingstation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.data.redis")
public class MyRedisProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private long ttl; // AI 对话记忆 TTL（秒）
}