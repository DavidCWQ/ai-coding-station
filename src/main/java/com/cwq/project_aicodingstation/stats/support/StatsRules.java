package com.cwq.project_aicodingstation.stats.support;

import java.time.ZoneId;

/**
 * 统计口径。在线窗口和缓存时间都落在需求允许的 1 到 5 分钟延迟里。
 */
public final class StatsRules {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final long PAGE_VIEW_DEDUP_MINUTES = 30;
    public static final long UV_TTL_HOURS = 48;
    public static final long OVERVIEW_CACHE_SECONDS = 60;
    public static final long ONLINE_WINDOW_MILLIS = 5 * 60 * 1000L;
    public static final int LOG_RETENTION_DAYS = 90;

    private StatsRules() {
    }
}
