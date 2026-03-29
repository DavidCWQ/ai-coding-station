package com.cwq.project_aicodingstation.app.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.ai.config.AICodeProperties;
import com.cwq.project_aicodingstation.app.config.AppDeployConfig;
import com.cwq.project_aicodingstation.app.constant.AppConstant;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.mapper.AppMapper;
import com.cwq.project_aicodingstation.app.service.AppPurgeService;
import com.cwq.project_aicodingstation.core.screenshot.ScreenshotConfig;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link AppPurgeService} 实现：物理删应用行（子表外键 CASCADE）后按约定路径清理磁盘。
 */
@Slf4j
@Service
public class AppPurgeServiceImpl implements AppPurgeService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private AICodeProperties aiCodeProperties;

    @Resource
    private AppDeployConfig appDeployConfig;

    @Resource
    private ScreenshotConfig screenshotConfig;

    @Override
    public void purgeExpiredSoftDeletedApps() {
        purgeExpiredSoftDeletedApps(AppConstant.SOFT_DELETED_RETENTION_DAYS);
    }

    @Override
    public void purgeExpiredSoftDeletedApps(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<App> apps = LogicDeleteManager.execWithoutLogicDelete(() -> {
            QueryWrapper qw = QueryWrapper.create();
            qw.where("is_deleted = ?", 1);
            qw.and("update_time < ?", cutoff);
            return appMapper.selectListByQuery(qw);
        });
        if (CollUtil.isEmpty(apps)) {
            log.debug("无待物理清理的已删除应用");
            return;
        }
        log.info("开始物理清理已过保留期的应用，共 {} 条", apps.size());
        for (App app : apps) {
            Long id = app.getId();
            try {
                LogicDeleteManager.execWithoutLogicDelete(() -> appMapper.deleteById(id));
                deleteArtifactsQuietly(app);
                log.info("已物理删除应用并清理文件, appId={}", id);
            } catch (Exception e) {
                log.error("物理删除应用失败, appId={}", id, e);
            }
        }
    }

    /**
     * 封面文件固定为 {@code cover_{appId}.png}；
     * <p>
     * 不按 DB {@code cover} URL 判断，避免「库中无 URL 但磁盘仍有截图」时漏删。
     * </p>
     */
    private void deleteArtifactsQuietly(App app) {
        try {
            if (StrUtil.isNotBlank(app.getCodeGenType())) {
                String genDir = aiCodeProperties.getOutputDir() + File.separator
                        + app.getCodeGenType() + "_" + app.getId();
                FileUtil.del(genDir);
            }
            if (StrUtil.isNotBlank(app.getDeployKey())) {
                String deployDir = appDeployConfig.getDeployDir() + File.separator + app.getDeployKey();
                FileUtil.del(deployDir);
            }
            String coverPath = screenshotConfig.getOutputDir() + File.separator + "cover_" + app.getId() + ".png";
            FileUtil.del(coverPath);
        } catch (Exception e) {
            log.warn("清理应用磁盘文件失败，appId={}, err={}", app.getId(), e.getMessage());
        }
    }
}
