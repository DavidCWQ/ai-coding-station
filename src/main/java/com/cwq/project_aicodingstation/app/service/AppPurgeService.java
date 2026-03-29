package com.cwq.project_aicodingstation.app.service;

/**
 * 软删除应用保留期满后的物理清理：库记录与磁盘产物（生成代码、部署目录、封面图）。
 * <p>
 * 保留期按逻辑删除行上的 {@code update_time} 判断。
 * </p>
 */
public interface AppPurgeService {

    void purgeExpiredSoftDeletedApps();

    /**
     * @param retentionDays 软删除后保留天数，期满则清理
     */
    void purgeExpiredSoftDeletedApps(int retentionDays);
}
