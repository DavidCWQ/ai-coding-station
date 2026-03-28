package com.cwq.project_aicodingstation.core.download;

import cn.hutool.core.util.ZipUtil;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/**
 * 基于 Hutool {@link ZipUtil} 的目录打包下载，支持按路径段与扩展名过滤。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName,
                                     HttpServletResponse response) {

        // 1. 基础校验
        BusinessAssert.notBlank(projectPath, ErrorCode.PARAMS_ERROR, "项目路径不能为空");
        BusinessAssert.notBlank(downloadFileName, ErrorCode.PARAMS_ERROR, "下载文件名不能为空");

        File projectDir = new File(projectPath);
        BusinessAssert.requireTrue(projectDir.exists(), ErrorCode.NOT_FOUND, "项目目录不存在");
        BusinessAssert.requireTrue(projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "指定路径不是目录");

        Path projectRoot = projectDir.toPath().toAbsolutePath().normalize();
        log.info("开始打包下载项目: {} -> {}.zip", projectRoot, downloadFileName);

        // 2. 设置 HTTP 响应头
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", downloadFileName));

        // 3. 文件过滤器
        FileFilter filter = file -> isPathAllowed(projectRoot, file.toPath().toAbsolutePath().normalize());

        try {
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, filter, projectDir);
            log.info("项目打包下载完成: {}", downloadFileName);
        } catch (Exception e) {
            log.error("项目打包下载异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目打包下载失败");
        }
    }

    /**
     * 检查路径是否允许包含在压缩包中（逐段匹配目录名与扩展名）。
     *
     * @param projectRoot 项目根目录
     * @param fullPath    完整路径
     * @return 是否允许打包该路径
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        Path relativePath = projectRoot.relativize(fullPath);
        for (Path part : relativePath) {
            String partName = part.toString();
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            for (String ext : IGNORED_EXTENSIONS) {
                if (partName.endsWith(ext)) {
                    return false;
                }
            }
        }
        return true;
    }
}
