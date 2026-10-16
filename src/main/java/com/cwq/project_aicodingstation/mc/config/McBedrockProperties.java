package com.cwq.project_aicodingstation.mc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "mc.bedrock")
public class McBedrockProperties {

    /**
     * 未配置 home 时控制台只返回 disabled，不影响网站其它功能。
     */
    private boolean enabled = false;

    /**
     * mc_bedrock 项目根目录（含 compose.yaml、data/、scripts/、backups/）。
     */
    private String home = "";

    private String containerName = "bedrock-server";

    /**
     * 为 true 且能执行 bash/docker 时，管理操作当场跑脚本；
     * 否则写入 ops/inbox，由宿主机 ops-worker 消费。
     */
    private boolean execScripts = true;

    private int opsTimeoutSeconds = 180;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public boolean isExecScripts() {
        return execScripts;
    }

    public void setExecScripts(boolean execScripts) {
        this.execScripts = execScripts;
    }

    public int getOpsTimeoutSeconds() {
        return opsTimeoutSeconds;
    }

    public void setOpsTimeoutSeconds(int opsTimeoutSeconds) {
        this.opsTimeoutSeconds = opsTimeoutSeconds;
    }

    public Path homePath() {
        return home == null || home.isBlank() ? null : Paths.get(home).toAbsolutePath().normalize();
    }
}
