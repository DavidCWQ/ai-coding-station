package com.cwq.project_aicodingstation.stats.dto;

import lombok.Data;

/**
 * 前端路由上报。appId 用字符串，避免雪花 id 在 JSON 数字里丢精度。
 */
@Data
public class VisitHitRequest {

    private String kind;
    private String scope;
    private String appId;
    private String path;
}
