package com.cwq.project_aicodingstation.stats.mapper;

import com.cwq.project_aicodingstation.stats.entity.VisitLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface VisitLogMapper extends BaseMapper<VisitLog> {

    /**
     * 全站访问按访客、按日去重。同一人同一天多条明细只留一行。
     */
    @Select("""
            SELECT visitor_hash AS visitorHash, stat_date AS statDate
            FROM visit_log
            WHERE scope = 'SITE'
              AND stat_date >= #{from}
              AND stat_date <= #{to}
            GROUP BY visitor_hash, stat_date
            """)
    List<VisitLog> listSiteVisitorDays(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
