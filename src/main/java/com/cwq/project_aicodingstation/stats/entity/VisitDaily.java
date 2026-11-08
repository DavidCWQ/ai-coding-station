package com.cwq.project_aicodingstation.stats.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 按日汇总的一行。全站的 appId 为 0。
 */
@Data
public class VisitDaily {

    private LocalDate statDate;
    private String scope;
    private Long appId;
    private Long pv;
    private Long uv;
}
