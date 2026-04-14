package com.cwq.project_aicodingstation.app.task;

import com.cwq.project_aicodingstation.app.service.AppPurgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 启动与定时清理：已满保留期的软删除应用（库记录 + 生成代码/部署目录/封面）。
 * <p>
 * 调度由 Spring {@link Scheduled} 执行，使用默认单线程调度器即可；无需自行 {@code new Thread}。
 * </p>
 */
@Slf4j
@Component
@Profile("!test")
public class DeletedAppPurgeJob implements ApplicationRunner {

    @Resource
    private AppPurgeService appPurgeService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("启动执行：已过保留期的已删除应用清理");
        appPurgeService.purgeExpiredSoftDeletedApps();
    }

    /**
     * 每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledPurge() {
        log.info("定时任务：已过保留期的已删除应用清理");
        appPurgeService.purgeExpiredSoftDeletedApps();
    }
}
