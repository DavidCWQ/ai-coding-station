package com.cwq.project_aicodingstation.stats.enums;

import java.util.Locale;

/**
 * 全站页面与应用详情分开计数，应用详情不写入全站汇总。
 */
public enum VisitScope {
    SITE,
    APP;

    public static VisitScope parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
