package com.cwq.project_aicodingstation.ai.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型调用的固定窗口限流。改配置后重启生效，与本站其他配置一致。
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class AiRateLimitProperties {

    /** 关闭后不再计数，本地排障时可临时关掉。 */
    private boolean enabled = true;

    /** 计数窗口，单位秒。 */
    private int windowSeconds = 60;

    /** 同一登录用户在窗口内最多发起的模型调用次数。小于等于 0 表示不限制用户。 */
    private int userPerWindow = 20;

    /** 同一客户端 IP 在窗口内最多发起的模型调用次数。小于等于 0 表示不限制 IP。 */
    private int ipPerWindow = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getUserPerWindow() {
        return userPerWindow;
    }

    public void setUserPerWindow(int userPerWindow) {
        this.userPerWindow = userPerWindow;
    }

    public int getIpPerWindow() {
        return ipPerWindow;
    }

    public void setIpPerWindow(int ipPerWindow) {
        this.ipPerWindow = ipPerWindow;
    }
}
