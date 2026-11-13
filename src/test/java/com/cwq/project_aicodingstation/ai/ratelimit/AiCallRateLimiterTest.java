package com.cwq.project_aicodingstation.ai.ratelimit;

import com.cwq.project_aicodingstation.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCallRateLimiterTest {

    @Mock
    private StringRedisTemplate redis;

    private AiRateLimitProperties properties;
    private AiCallRateLimiter limiter;

    @BeforeEach
    void setUp() {
        properties = new AiRateLimitProperties();
        limiter = new AiCallRateLimiter(redis, properties);
    }

    @Test
    void twentiethCallIsAllowedAndTwentyFirstIsRejected() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(20L, 1L);
        assertDoesNotThrow(() -> limiter.acquire(7L));

        when(redis.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(21L);
        BusinessException ex = assertThrows(BusinessException.class, () -> limiter.acquire(7L));
        org.junit.jupiter.api.Assertions.assertEquals(AiCallRateLimiter.USER_MESSAGE, ex.getMessage());
    }

    @Test
    void ipLimitRejectsEvenWhenUserIsUnderLimit() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(1L, 61L);

        assertThrows(BusinessException.class, () -> limiter.acquire(7L));
    }

    @Test
    void disabledLimiterDoesNotTouchRedis() {
        properties.setEnabled(false);

        limiter.acquire(7L);

        verify(redis, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void redisFailureFailsOpen() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new IllegalStateException("redis down"));

        assertDoesNotThrow(() -> limiter.acquire(7L));
    }

    @Test
    void zeroUserLimitSkipsTheUserBucket() {
        properties.setUserPerWindow(0);
        properties.setIpPerWindow(0);

        limiter.acquire(7L);

        verify(redis, never()).execute(any(RedisScript.class), anyList(), anyString());
    }
}
