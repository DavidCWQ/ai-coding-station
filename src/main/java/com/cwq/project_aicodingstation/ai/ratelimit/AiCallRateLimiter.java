package com.cwq.project_aicodingstation.ai.ratelimit;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.user.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 用 Redis 固定窗口限制会打到大模型的接口。
 * 登录和重置密码已有独立计数，这里只罩住生成代码、智能体对话和应用创建。
 */
@Slf4j
@Component
public class AiCallRateLimiter {

    static final String USER_MESSAGE = "操作太频繁，请稍后再试";

    private static final String SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            else
              local ttl = redis.call('PTTL', KEYS[1])
              if ttl < 0 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
              end
            end
            return current
            """;

    private final StringRedisTemplate redis;
    private final AiRateLimitProperties properties;
    private final DefaultRedisScript<Long> script;

    public AiCallRateLimiter(StringRedisTemplate redis, AiRateLimitProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.script = new DefaultRedisScript<>(SCRIPT, Long.class);
    }

    public void acquire(Long userId) {
        if (!properties.isEnabled()) {
            return;
        }
        int windowSeconds = Math.max(properties.getWindowSeconds(), 1);
        long windowMillis = windowSeconds * 1000L;
        String ip = currentIp();
        int userLimit = properties.getUserPerWindow();
        if (userLimit > 0 && userId != null) {
            hit("rate:ai:user:" + userId, userLimit, windowMillis, "user", userId, ip);
        }
        int ipLimit = properties.getIpPerWindow();
        if (ipLimit > 0) {
            hit("rate:ai:ip:" + ip, ipLimit, windowMillis, "ip", userId, ip);
        }
    }

    private void hit(String key, int limit, long windowMillis, String bucket, Long userId, String ip) {
        Long count;
        try {
            count = redis.execute(script, List.of(key), String.valueOf(windowMillis));
        } catch (RuntimeException ex) {
            log.error("AI 限流计数失败，本次放行 key={}", key, ex);
            return;
        }
        if (count == null) {
            log.error("AI 限流计数没有返回，本次放行 key={}", key);
            return;
        }
        if (count > limit) {
            log.warn("AI 调用超过频率上限 userId={} ip={} bucket={} count={} limit={}",
                    userId, ip, bucket, count, limit);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, USER_MESSAGE);
        }
    }

    private static String currentIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        if (request == null) {
            return "unknown";
        }
        return ClientIpResolver.resolve(request);
    }
}
