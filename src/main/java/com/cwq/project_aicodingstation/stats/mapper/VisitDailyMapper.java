package com.cwq.project_aicodingstation.stats.mapper;

import com.cwq.project_aicodingstation.stats.entity.VisitDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface VisitDailyMapper {

    @Insert("""
            INSERT INTO visit_daily (stat_date, scope, app_id, pv, uv)
            VALUES (#{statDate}, #{scope}, #{appId}, 1, #{uvDelta})
            ON DUPLICATE KEY UPDATE pv = pv + 1, uv = uv + #{uvDelta}
            """)
    int upsertHit(@Param("statDate") LocalDate statDate,
                  @Param("scope") String scope,
                  @Param("appId") long appId,
                  @Param("uvDelta") int uvDelta);

    @Select("""
            SELECT stat_date AS statDate, scope, app_id AS appId, pv, uv
            FROM visit_daily
            WHERE stat_date >= #{from} AND stat_date <= #{to}
            """)
    List<VisitDaily> listBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            <script>
            SELECT app_id AS appId, SUM(pv) AS pv
            FROM visit_daily
            WHERE scope = 'APP' AND app_id IN
            <foreach collection="appIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            GROUP BY app_id
            </script>
            """)
    List<VisitDaily> sumAppPageViews(@Param("appIds") List<Long> appIds);
}
