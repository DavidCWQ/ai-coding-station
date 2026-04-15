package com.cwq.project_aicodingstation.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Set;

@ConfigurationProperties(prefix = "app.asset")
@Component
@Data
public class AppAssetConfig {

    public static final String IMAGE_WEB_ROOT = "/img/";

    public static final String IMAGE_WEB_ROOT_NO_SLASH = "img";

    public static final Set<String> ALLOWED_IMAGE_MIME = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    public static final Set<String> ALLOWED_IMAGE_EXT = Set.of(
            "png",
            "jpg",
            "jpeg",
            "webp",
            "gif",
            "svg"
    );

    public static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * 推荐图片大小上限（字节），超过该值会自动压缩到该阈值以内，默认 2MB。
     */
    private long preferredImageSizeBytes = 2L * 1024 * 1024;

    /**
     * 是否开启超限自动压缩。
     */
    private boolean autoCompressOversizeImage = true;

    /**
     * JPEG 压缩质量 (0~1)，默认 0.82。
     */
    private float jpegCompressionQuality = 0.82f;

    /**
     * 单应用最多允许上传的图片数量。
     */
    private int maxImageCountPerApp = 200;

    /**
     * 单应用图片总容量上限（字节），默认 100MB。
     */
    private long maxTotalImageSizeBytesPerApp = 100L * 1024 * 1024;
}
