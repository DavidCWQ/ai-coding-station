package com.cwq.project_aicodingstation.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.deploy")
@Component
@Data
public class AppDeployConfig {

    /**
     * 生成代码的输出根目录，output-dir -> outputDir，即 kebab-case -> camelCase 映射
     */
    private String outputDir = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 生成代码的输出根目录，deploy-dir -> deployDir，即 kebab-case -> camelCase 映射
     */
    private String deployDir = System.getProperty("user.dir") + "/tmp/code_deploy";

    private String deployHost = "http://localhost";
}
