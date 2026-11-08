package com.cwq.project_aicodingstation.stats.enums;

import java.util.Locale;

/**
 * 一次上报是新的页面浏览，还是只刷新在线状态。
 */
public enum VisitKind {
    VIEW,
    HEARTBEAT;

    public static VisitKind parse(String raw) {
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
