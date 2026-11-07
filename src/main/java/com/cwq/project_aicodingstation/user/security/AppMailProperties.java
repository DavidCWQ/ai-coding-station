package com.cwq.project_aicodingstation.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    /** 显式打开后才发送重置邮件。 */
    private boolean enabled = false;

    private String host = "";

    private int port = 587;

    private String username = "";

    private String password = "";

    /** 发件人地址，一般与 username 相同。 */
    private String from = "";

    /** 重置链接的站点根地址，不含末尾斜杠。 */
    private String resetBaseUrl = "http://localhost:5173";

    private int tokenTtlMinutes = 30;

    public AppMailProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getResetBaseUrl() {
        return resetBaseUrl;
    }

    public void setResetBaseUrl(String resetBaseUrl) {
        this.resetBaseUrl = resetBaseUrl;
    }

    public int getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }

    public void setTokenTtlMinutes(int tokenTtlMinutes) {
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public boolean isReady() {
        return enabled
                && hasText(host)
                && hasText(username)
                && hasText(password)
                && hasText(from)
                && hasText(resetBaseUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
