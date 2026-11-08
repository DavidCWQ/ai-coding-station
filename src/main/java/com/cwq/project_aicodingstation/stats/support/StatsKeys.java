package com.cwq.project_aicodingstation.stats.support;

import java.time.LocalDate;

/**
 * 统计用的 Redis 键。UV 按天、按目标分开；会话去重键不含日期，靠 30 分钟过期。
 */
public final class StatsKeys {

    private StatsKeys() {
    }

    public static String uv(LocalDate date, String scope, long appId) {
        return "stats:uv:" + date + ":" + scope + ":" + appId;
    }

    public static String pageViewDedup(String sessionHash, String scope, long appId) {
        return "stats:pv:" + sessionHash + ":" + scope + ":" + appId;
    }

    public static String online() {
        return "stats:online";
    }

    public static String overview(int days) {
        return "stats:overview:" + days;
    }
}
