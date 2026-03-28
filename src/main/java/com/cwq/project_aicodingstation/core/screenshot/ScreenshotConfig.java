package com.cwq.project_aicodingstation.core.screenshot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.screenshot")
@Data
public class ScreenshotConfig {

    /**
     * Node 脚本路径
     */
    private String scriptPath;

    /**
     * 封面保存目录
     */
    private String outputDir;

    /**
     * nginx 访问前缀
     */
    private String baseUrl;
}
