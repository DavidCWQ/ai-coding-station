package com.cwq.project_aicodingstation.stats.task;

import com.cwq.project_aicodingstation.stats.service.StatsService;
import com.cwq.project_aicodingstation.stats.support.StatsRules;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每天凌晨清理超过保留期的访问明细。汇总表不删。
 */
@Slf4j
@Component
@Profile("!test")
public class VisitLogPurgeJob {

    @Resource
    private StatsService statsService;

    @Scheduled(cron = "0 30 2 * * ?")
    public void purgeExpiredVisitLogs() {
        int removed = statsService.purgeExpiredLogs();
        log.info("访问明细清理完成，删除 {} 条超过 {} 天的记录", removed, StatsRules.LOG_RETENTION_DAYS);
    }
}
