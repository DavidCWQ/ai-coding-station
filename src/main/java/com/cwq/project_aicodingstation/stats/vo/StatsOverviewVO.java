package com.cwq.project_aicodingstation.stats.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StatsOverviewVO {

    private long todayPv;
    private long todayUv;
    private long online;
    /** 所选天数内的全站独立访客。同一个人来多天只计一次。 */
    private long periodUv;
    /** 所选天数内至少来过两天的全站访客。 */
    private long returningUv;
    private int days;
    private List<StatsTrendPointVO> trend = new ArrayList<>();
    private List<StatsAppRankVO> ranking = new ArrayList<>();
}
