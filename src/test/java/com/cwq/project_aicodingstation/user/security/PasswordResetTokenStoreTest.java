package com.cwq.project_aicodingstation.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenStoreTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    private final Map<String, String> store = new HashMap<>();

    private PasswordResetTokenStore tokenStore;

    @BeforeEach
    void setUp() {
        AppMailProperties properties = new AppMailProperties();
        properties.setTokenTtlMinutes(30);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            store.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        doAnswer(invocation -> store.remove(invocation.getArgument(0)) != null)
                .when(redis).delete(anyString());

        tokenStore = new PasswordResetTokenStore(redis, properties);
    }

    @Test
    void tokenIsSingleUseAndStoredAsHash() {
        String raw = tokenStore.issue(42L);

        assertNull(store.get("auth:pwd-reset:token:" + raw));
        assertEquals("42", store.get("auth:pwd-reset:token:" + PasswordResetTokenStore.sha256(raw)));

        assertEquals(42L, tokenStore.consume(raw));
        assertNull(tokenStore.consume(raw));
    }

    @Test
    void newTokenInvalidatesThePreviousOne() {
        String first = tokenStore.issue(7L);
        String second = tokenStore.issue(7L);

        assertNotEquals(first, second);
        assertNull(tokenStore.consume(first));
        assertEquals(7L, tokenStore.consume(second));
    }
}
