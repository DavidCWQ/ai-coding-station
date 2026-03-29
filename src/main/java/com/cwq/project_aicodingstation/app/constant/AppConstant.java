package com.cwq.project_aicodingstation.app.constant;

public interface AppConstant {

    /// 精选应用优先级
    Integer GOOD_APP_PRIORITY = 99;

    /// 默认应用优先级
    Integer DEFAULT_APP_PRIORITY = 0;

    /// 默认最大翻页数
    Long MAX_PAGE_SIZE = 20L;

    /// 软删除保留天数，期满后启动/定时任务物理删除并清理磁盘
    int SOFT_DELETED_RETENTION_DAYS = 14;

}
