package com.cwq.project_aicodingstation.ai.ratelimit;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对本地 Redis 做饱和测试。没有 Redis 时跳过，不连 DeepSeek，也不走网页。
 */
class AiCallRateLimiterRedisTest {

    private static final long USER_ID = 424242424242L;

    private JedisConnectionFactory factory;
    private StringRedisTemplate redis;
    private AiCallRateLimiter limiter;
    private AiRateLimitProperties properties;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6380);
        config.setUsername("default");
        config.setPassword(RedisPassword.of("redis_secret"));
        factory = new JedisConnectionFactory(config);
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        try {
            String pong = redis.getConnectionFactory().getConnection().ping();
            Assumptions.assumeTrue("PONG".equalsIgnoreCase(pong), "local redis unavailable");
        } catch (RuntimeException ex) {
            Assumptions.abort("local redis unavailable");
        }
        redis.delete(List.of(userKey(), ipKey()));
        properties = new AiRateLimitProperties();
        limiter = new AiCallRateLimiter(redis, properties);
    }

    @AfterEach
    void tearDown() {
        if (redis != null) {
            redis.delete(List.of(userKey(), ipKey()));
        }
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    void sameUserIsRejectedAfterTwentyAndTheWindowHasATtl() {
        properties.setUserPerWindow(20);
        properties.setIpPerWindow(60);

        int allowed = 0;
        int rejected = 0;
        for (int i = 0; i < 30; i++) {
            try {
                limiter.acquire(USER_ID);
                allowed++;
            } catch (BusinessException ex) {
                rejected++;
                assertEquals(ErrorCode.TOO_MANY_REQUESTS.getCode(), ex.getCode());
                assertEquals(AiCallRateLimiter.USER_MESSAGE, ex.getMessage());
            }
        }

        assertEquals(20, allowed);
        assertEquals(10, rejected);
        Long ttlMillis = redis.getExpire(userKey(), TimeUnit.MILLISECONDS);
        assertTrue(ttlMillis != null && ttlMillis > 0 && ttlMillis <= 60_000,
                "ttlMillis=" + ttlMillis);
        assertEquals("30", redis.opsForValue().get(userKey()));
    }

    @Test
    void counterStartsOverAfterTheWindowExpires() throws InterruptedException {
        properties.setWindowSeconds(2);
        properties.setUserPerWindow(2);
        properties.setIpPerWindow(0);

        limiter.acquire(USER_ID);
        limiter.acquire(USER_ID);
        BusinessException blocked = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class, () -> limiter.acquire(USER_ID));
        assertEquals(AiCallRateLimiter.USER_MESSAGE, blocked.getMessage());

        Thread.sleep(2500);

        limiter.acquire(USER_ID);
        assertEquals("1", redis.opsForValue().get(userKey()));
    }

    private static String userKey() {
        return "rate:ai:user:" + USER_ID;
    }

    private static String ipKey() {
        return "rate:ai:ip:unknown";
    }
}
