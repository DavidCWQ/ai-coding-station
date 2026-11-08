package com.cwq.project_aicodingstation.stats.support;

import java.util.regex.Pattern;

/**
 * 爬虫和空 User-Agent 不计入浏览量、独立访客和在线人数。
 */
public final class BotUserAgents {

    private static final Pattern BOT = Pattern.compile(
            "(?i)(bot|spider|crawler|slurp|bingpreview|facebookexternalhit|curl|wget|python-requests|go-http-client|headless|scrapy)");

    private BotUserAgents() {
    }

    public static boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        String sample = userAgent.length() > 1024 ? userAgent.substring(0, 1024) : userAgent;
        return BOT.matcher(sample).find();
    }
}
