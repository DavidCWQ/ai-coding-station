package com.cwq.project_aicodingstation.stats.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 概览缓存的 JSON。日期写成 ISO 字符串，方便前端直接显示。
 */
public final class StatsJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private StatsJson() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
