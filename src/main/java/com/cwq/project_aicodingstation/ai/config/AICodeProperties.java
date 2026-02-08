package com.cwq.project_aicodingstation.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ai.code")
@Component
@Data
public class AICodeProperties {

    /**
     * 生成代码的输出根目录，output-dir -> outputDir，即 kebab-case -> camelCase 映射
     */
    private String outputDir = System.getProperty("user.dir") + "/tmp/code_output";
}
