package com.cwq.project_aicodingstation.user.security;

import com.cwq.project_aicodingstation.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthThrottleTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    private AuthThrottle throttle;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(values);
        throttle = new AuthThrottle(redis);
    }

    @Test
    void loginLockReportsRemainingMinutes() {
        String lockKey = AuthThrottle.accountLockKey("david", "1.2.3.4");
        when(redis.getExpire(eq(lockKey), eq(TimeUnit.SECONDS))).thenReturn(90L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> throttle.assertLoginAllowed("david", "1.2.3.4"));

        assertTrue(ex.getMessage().contains("2 分钟"));
    }

    @Test
    void fifthFailureLocksAccountAndIpWindowStarts() {
        String failKey = AuthThrottle.accountFailKey("david", "1.2.3.4");
        String ipFailKey = AuthThrottle.ipFailKey("1.2.3.4");
        when(values.increment(failKey)).thenReturn(5L);
        when(values.increment(ipFailKey)).thenReturn(1L);

        assertTrue(throttle.recordLoginFailure("david", "1.2.3.4"));

        verify(values).set(eq(AuthThrottle.accountLockKey("david", "1.2.3.4")), eq("1"), eq(10L), eq(TimeUnit.MINUTES));
        verify(redis).delete(failKey);
        verify(redis).expire(ipFailKey, 10L, TimeUnit.MINUTES);
    }

    @Test
    void firstFailureOnlyCounts() {
        String failKey = AuthThrottle.accountFailKey("david", "1.2.3.4");
        String ipFailKey = AuthThrottle.ipFailKey("1.2.3.4");
        when(values.increment(failKey)).thenReturn(1L);
        when(values.increment(ipFailKey)).thenReturn(1L);

        assertFalse(throttle.recordLoginFailure("david", "1.2.3.4"));

        verify(redis).expire(failKey, 24L, TimeUnit.HOURS);
        verify(redis).expire(ipFailKey, 10L, TimeUnit.MINUTES);
    }

    @Test
    void forgotPasswordStopsAfterAccountLimit() {
        when(values.increment("auth:reset:account:david")).thenReturn(4L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> throttle.assertForgotAllowed("david", "1.2.3.4"));

        assertTrue(ex.getMessage().contains("请求过于频繁"));
    }

    @Test
    void directClientIpIgnoresSpoofedForwardedHeader() {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("X-Real-IP", "1.1.1.1");

        org.junit.jupiter.api.Assertions.assertEquals("8.8.8.8", ClientIpResolver.resolve(request));
    }

    @Test
    void proxyHopUsesRealIpHeader() {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("X-Real-IP", "203.0.113.9");

        org.junit.jupiter.api.Assertions.assertEquals("203.0.113.9", ClientIpResolver.resolve(request));
    }

    @Test
    void emailNormalize() {
        org.junit.jupiter.api.Assertions.assertEquals("a@b.co", EmailAddresses.normalize(" A@B.co "));
        org.junit.jupiter.api.Assertions.assertNull(EmailAddresses.normalize("not-an-email"));
        org.junit.jupiter.api.Assertions.assertEquals("david", AuthThrottle.accountKey(" David "));
    }
}
