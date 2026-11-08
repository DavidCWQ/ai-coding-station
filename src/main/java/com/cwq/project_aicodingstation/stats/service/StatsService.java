package com.cwq.project_aicodingstation.stats.service;

import com.cwq.project_aicodingstation.stats.dto.VisitHitRequest;
import com.cwq.project_aicodingstation.stats.vo.StatsOverviewVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.Map;

public interface StatsService {

    /**
     * @return 这次上报是否新计入了一次浏览量
     */
    boolean record(VisitHitRequest request, HttpServletRequest httpRequest);

    StatsOverviewVO overview(int days);

    /**
     * 每个应用的累计浏览量。没有记录的应用不会出现在结果里。
     */
    Map<Long, Long> appPageViews(Collection<Long> appIds);

    /**
     * @return 删除的明细行数
     */
    int purgeExpiredLogs();
}
