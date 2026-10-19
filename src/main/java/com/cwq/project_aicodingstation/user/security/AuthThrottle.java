package com.cwq.project_aicodingstation.user.security;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 登录与找回密码的 Redis 计数。
 * 登录按「账号 + IP」连续失败 5 次锁定 10 分钟，同一 IP 在 10 分钟内失败满 30 次同样锁定。
 */
@Service
public class AuthThrottle {

    static final int LOGIN_FAIL_LIMIT = 5;
    static final int IP_FAIL_LIMIT = 30;
    static final int FORGOT_ACCOUNT_LIMIT = 3;
    static final int FORGOT_IP_LIMIT = 10;
    static final int RESET_IP_LIMIT = 10;

    private static final long LOCK_MINUTES = 10;
    private static final long FAIL_COUNTER_HOURS = 24;
    private static final long RESET_WINDOW_MINUTES = 15;

    private final StringRedisTemplate redis;

    public AuthThrottle(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public static String accountKey(String account) {
        if (account == null) {
            return "";
        }
        return account.trim().toLowerCase(Locale.ROOT);
    }

    public void assertLoginAllowed(String accountKey, String ip) {
        throwIfLocked(accountLockKey(accountKey, ip));
        throwIfLocked(ipLockKey(ip));
    }

    /**
     * @return 这次失败是否触发了锁定
     */
    public boolean recordLoginFailure(String accountKey, String ip) {
        boolean locked = false;

        String failKey = accountFailKey(accountKey, ip);
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count < LOGIN_FAIL_LIMIT) {
            redis.expire(failKey, FAIL_COUNTER_HOURS, TimeUnit.HOURS);
        }
        if (count != null && count >= LOGIN_FAIL_LIMIT) {
            redis.opsForValue().set(accountLockKey(accountKey, ip), "1", LOCK_MINUTES, TimeUnit.MINUTES);
            redis.delete(failKey);
            locked = true;
        }

        String ipFailKey = ipFailKey(ip);
        Long ipCount = redis.opsForValue().increment(ipFailKey);
        if (ipCount != null && ipCount == 1L) {
            redis.expire(ipFailKey, LOCK_MINUTES, TimeUnit.MINUTES);
        }
        if (ipCount != null && ipCount >= IP_FAIL_LIMIT) {
            redis.opsForValue().set(ipLockKey(ip), "1", LOCK_MINUTES, TimeUnit.MINUTES);
            redis.delete(ipFailKey);
            locked = true;
        }
        return locked;
    }

    public void clearLoginFailures(String accountKey, String ip) {
        redis.delete(accountFailKey(accountKey, ip));
        redis.delete(accountLockKey(accountKey, ip));
    }

    public void assertForgotAllowed(String accountKey, String ip) {
        hit(forgotAccountKey(accountKey), FORGOT_ACCOUNT_LIMIT, RESET_WINDOW_MINUTES);
        hit(forgotIpKey(ip), FORGOT_IP_LIMIT, RESET_WINDOW_MINUTES);
    }

    public void assertResetSubmitAllowed(String ip) {
        hit(resetIpKey(ip), RESET_IP_LIMIT, RESET_WINDOW_MINUTES);
    }

    public void throwLoginLocked(String accountKey, String ip) {
        throwIfLocked(accountLockKey(accountKey, ip));
        throwIfLocked(ipLockKey(ip));
        throw locked(LOCK_MINUTES * 60);
    }

    private void hit(String key, int limit, long windowMinutes) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, windowMinutes, TimeUnit.MINUTES);
        }
        if (count != null && count > limit) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请求过于频繁，请稍后再试");
        }
    }

    private void throwIfLocked(String key) {
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < -1) {
            return;
        }
        long seconds = ttl < 0 ? LOCK_MINUTES * 60 : ttl;
        throw locked(seconds);
    }

    private static BusinessException locked(long seconds) {
        long minutes = Math.max(1, (seconds + 59) / 60);
        return new BusinessException(ErrorCode.BUSINESS_ERROR, "尝试次数过多，请 " + minutes + " 分钟后再试");
    }

    static String accountFailKey(String accountKey, String ip) {
        return "auth:login:fail:" + accountKey + ":" + ip;
    }

    static String accountLockKey(String accountKey, String ip) {
        return "auth:login:lock:" + accountKey + ":" + ip;
    }

    static String ipFailKey(String ip) {
        return "auth:login:ip-fail:" + ip;
    }

    static String ipLockKey(String ip) {
        return "auth:login:ip-lock:" + ip;
    }

    private static String forgotAccountKey(String accountKey) {
        return "auth:reset:account:" + accountKey;
    }

    private static String forgotIpKey(String ip) {
        return "auth:reset:ip:" + ip;
    }

    private static String resetIpKey(String ip) {
        return "auth:reset:submit:" + ip;
    }
}
