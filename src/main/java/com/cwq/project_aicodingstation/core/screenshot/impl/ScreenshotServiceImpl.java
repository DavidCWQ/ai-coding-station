package com.cwq.project_aicodingstation.core.screenshot.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import com.cwq.project_aicodingstation.core.screenshot.ScreenshotConfig;
import com.cwq.project_aicodingstation.core.screenshot.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private ScreenshotConfig screenshotConfig;

    @Override
    public String capture(String url, Long appId) {
        try {
            String fileName = "cover_" + appId + ".png";
            String coverUrl = screenshotConfig.getBaseUrl() + "/" + fileName;

            File outputDir = new File(screenshotConfig.getOutputDir());
            if (!outputDir.exists()) {
                FileUtil.mkdir(outputDir);
            }
            String outputPath = screenshotConfig.getOutputDir() + "/" + fileName;

            log.info("开始生成网页截图, url={}", coverUrl);
            int exitCode = runScreenshotScript(url, outputPath);

            if (exitCode != 0) {
                log.error("截图失败, exitCode={}", exitCode);
                return null;
            }
            compressImage(outputPath, outputPath);

            log.info("截图成功: {}", coverUrl);
            return coverUrl;
        } catch (Exception e) {
            log.error("截图异常", e);
            return null;
        }
    }

    private int runScreenshotScript(String url, String outputPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "node",
                screenshotConfig.getScriptPath(),
                url,
                outputPath
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        return process.waitFor();
    }

    private static void compressImage(String originalImgPath, String compressedImgPath) {
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImgPath),
                    FileUtil.file(compressedImgPath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败: {} -> {}", originalImgPath, compressedImgPath, e);
        }
    }
}
