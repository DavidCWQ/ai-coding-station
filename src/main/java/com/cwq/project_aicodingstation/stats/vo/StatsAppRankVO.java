package com.cwq.project_aicodingstation.stats.vo;

import lombok.Data;

@Data
public class StatsAppRankVO {

    private String appId;
    private String appName;
    private long pv;
    private long uv;
}
