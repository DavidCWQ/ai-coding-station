package com.cwq.project_aicodingstation.stats.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StatsOverviewVO {

    private long todayPv;
    private long todayUv;
    private long online;
    private int days;
    private List<StatsTrendPointVO> trend = new ArrayList<>();
    private List<StatsAppRankVO> ranking = new ArrayList<>();
}
