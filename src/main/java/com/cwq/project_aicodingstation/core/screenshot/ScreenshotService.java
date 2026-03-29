package com.cwq.project_aicodingstation.core.screenshot;

/**
 * 网页截图生成封面图（本地落盘 + 返回访问 URL）。
 */
public interface ScreenshotService {

    /**
     * 截图（异步调用以避免阻塞）
     *
     * @param url   截图页面 URL
     * @param appId 应用 ID
     * @return 访问 URL，失败返回 {@code null}
     */
    String capture(String url, Long appId);
}
