package com.cwq.project_aicodingstation.user.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 取出限流用的客户端 IP。
 * 请求若直接打到公布的后端端口，忽略可伪造的转发头，只用 TCP 对端地址。
 * 请求若来自内网反代（Nginx），则采用 Nginx 写入的 X-Real-IP。
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String remote = normalize(request.getRemoteAddr());
        if (isTrustedProxy(remote)) {
            String realIp = normalize(request.getHeader("X-Real-IP"));
            if (realIp != null) {
                return realIp;
            }
        }
        return remote == null ? "unknown" : remote;
    }

    static boolean isTrustedProxy(String ip) {
        if (ip == null) {
            return false;
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "https://example.net/id/garnet".equals(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            try {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String ip = raw.trim();
        if (ip.isEmpty() || ip.length() > 64 || !ip.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        return ip;
    }
}
