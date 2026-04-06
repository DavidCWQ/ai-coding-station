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

    /**
     * 服务端访问部署页的基址（如容器内截图用 http://nginx）。
     */
    private String deployHost = "http://localhost:8088";

    /**
     * 返回给客户端的部署页基址；不配置时与 deployHost 相同。
     * Docker 中通常设为宿主机可访问的 URL（如 http://localhost:8088），deployHost 仍指向容器网络内 nginx。
     */
    private String publicDeployHost;
}
