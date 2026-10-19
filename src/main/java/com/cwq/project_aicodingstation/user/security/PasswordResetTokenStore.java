package com.cwq.project_aicodingstation.user.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 重置令牌只把哈希放进 Redis，邮件里才出现原文。同一用户再次申请会作废上一封。
 */
@Service
public class PasswordResetTokenStore {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final AppMailProperties mailProperties;

    public PasswordResetTokenStore(StringRedisTemplate redis, AppMailProperties mailProperties) {
        this.redis = redis;
        this.mailProperties = mailProperties;
    }

    public String issue(long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = sha256(raw);
        Duration ttl = Duration.ofMinutes(Math.max(1, mailProperties.getTokenTtlMinutes()));

        String previous = redis.opsForValue().get(userKey(userId));
        if (previous != null && !previous.isBlank()) {
            redis.delete(tokenKey(previous));
        }
        redis.opsForValue().set(tokenKey(hash), Long.toString(userId), ttl);
        redis.opsForValue().set(userKey(userId), hash, ttl);
        return raw;
    }

    /**
     * 校验并消费令牌。无效、过期或已使用时返回 null。
     */
    public Long consume(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        String raw = rawToken.trim();
        if (raw.isEmpty() || raw.length() > 200) {
            return null;
        }
        String hash = sha256(raw);
        String userIdText = redis.opsForValue().get(tokenKey(hash));
        if (userIdText == null || userIdText.isBlank()) {
            return null;
        }
        long userId;
        try {
            userId = Long.parseLong(userIdText);
        } catch (NumberFormatException ex) {
            redis.delete(tokenKey(hash));
            return null;
        }
        String currentHash = redis.opsForValue().get(userKey(userId));
        redis.delete(tokenKey(hash));
        redis.delete(userKey(userId));
        if (!hash.equals(currentHash)) {
            return null;
        }
        return userId;
    }

    static String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static String tokenKey(String hash) {
        return "auth:pwd-reset:token:" + hash;
    }

    private static String userKey(long userId) {
        return "auth:pwd-reset:user:" + userId;
    }
}
