package com.cwq.project_aicodingstation.app.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.cwq.project_aicodingstation.app.config.AppAssetConfig;
import com.cwq.project_aicodingstation.app.config.AppDeployConfig;
import com.cwq.project_aicodingstation.app.dto.AppReplaceImageRequest;
import com.cwq.project_aicodingstation.app.entity.App;
import com.cwq.project_aicodingstation.app.service.AppAssetService;
import com.cwq.project_aicodingstation.app.service.AppService;
import com.cwq.project_aicodingstation.common.auth.ResourceAuthHelper;
import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class AppAssetServiceImpl implements AppAssetService {

    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1920;
    private static final float MIN_JPEG_QUALITY = 0.55f;

    private record PreparedImage(byte[] bytes, String ext) {
    }

    @Resource
    private AppService appService;

    @Resource
    private ResourceAuthHelper resourceAuthHelper;

    @Resource
    private AppDeployConfig appDeployConfig;

    @Resource
    private AppAssetConfig appAssetConfig;

    @Override
    public String uploadImage(Long appId, Long sessionId, MultipartFile file, UserLoginVO userVO) {
        resourceAuthHelper.requireAppEditable(appId, userVO);

        App app = appService.getById(appId);
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");

        BusinessAssert.notNull(file, ErrorCode.PARAMS_MISSING, "图片文件不能为空");
        BusinessAssert.requireTrue(!file.isEmpty(), ErrorCode.PARAMS_ERROR, "图片文件不能为空");

        String ext = normalizeImageExt(file.getOriginalFilename());
        validateImageFile(file.getContentType(), ext);
        PreparedImage preparedImage = prepareImageForUpload(file, ext);

        String appCodeDir = buildAppCodeDir(app);
        validateAppImageQuota(appCodeDir, preparedImage.bytes().length);
        String day = LocalDate.now().format(AppAssetConfig.DAY_FMT);
        String relativeDir = AppAssetConfig.IMAGE_WEB_ROOT_NO_SLASH + File.separator + day;
        String randomName = RandomUtil.randomString(24) + "." + preparedImage.ext();
        String relativePath = relativeDir + File.separator + randomName;
        String absolutePath = appCodeDir + File.separator + relativePath;
        try {
            FileUtil.mkParentDirs(absolutePath);
            FileUtil.writeBytes(preparedImage.bytes(), absolutePath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
        }

        String webPath = relativePath.replace("\\", "/");
        log.info("app image uploaded, appId={}, sessionId={}, userId={}, path={}",
                appId, sessionId, userVO.getId(), absolutePath);
        return webPath;
    }

    private PreparedImage prepareImageForUpload(MultipartFile file, String ext) {
        byte[] originalBytes;
        try {
            originalBytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传图片失败");
        }
        long preferredSize = appAssetConfig.getPreferredImageSizeBytes();
        if (originalBytes.length <= preferredSize) {
            return new PreparedImage(originalBytes, ext);
        }
        BusinessAssert.requireTrue(appAssetConfig.isAutoCompressOversizeImage(),
                ErrorCode.PARAMS_ERROR, "图片超过 2MB，请压缩后重试");
        BusinessAssert.requireTrue(canAttemptCompression(ext),
                ErrorCode.PARAMS_ERROR, "该图片格式暂不支持自动压缩，请将图片控制在 2MB 以内");

        byte[] compressed = tryCompressToPreferredSize(originalBytes, preferredSize);
        BusinessAssert.requireTrue(compressed.length <= preferredSize,
                ErrorCode.PARAMS_ERROR, "图片压缩后仍超过 2MB，请上传更小图片");
        return new PreparedImage(compressed, "jpg");
    }

    private boolean canAttemptCompression(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "webp".equals(ext);
    }

    private byte[] tryCompressToPreferredSize(byte[] originalBytes, long preferredSize) {
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(originalBytes));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片解析失败，无法自动压缩");
        }
        BusinessAssert.notNull(source, ErrorCode.PARAMS_ERROR, "图片解析失败，无法自动压缩");

        BufferedImage normalized = normalizeImage(source);
        float quality = Math.max(MIN_JPEG_QUALITY, appAssetConfig.getJpegCompressionQuality());
        byte[] compressed = encodeJpeg(normalized, quality);
        if (compressed.length <= preferredSize) {
            return compressed;
        }

        BufferedImage current = normalized;
        for (int i = 0; i < 4 && compressed.length > preferredSize; i++) {
            int nextW = Math.max(320, (int) (current.getWidth() * 0.85));
            int nextH = Math.max(320, (int) (current.getHeight() * 0.85));
            current = resizeImage(current, nextW, nextH);
            compressed = encodeJpeg(current, quality);
            if (quality > MIN_JPEG_QUALITY) {
                quality = Math.max(MIN_JPEG_QUALITY, quality - 0.08f);
                compressed = encodeJpeg(current, quality);
            }
        }
        return compressed;
    }

    private BufferedImage normalizeImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double ratio = Math.min((double) MAX_IMAGE_WIDTH / width, (double) MAX_IMAGE_HEIGHT / height);
        BufferedImage input = source;
        if (ratio < 1.0d) {
            int newW = Math.max(1, (int) Math.round(width * ratio));
            int newH = Math.max(1, (int) Math.round(height * ratio));
            input = resizeImage(source, newW, newH);
        }
        BufferedImage rgb = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(input, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
            return baos.toByteArray();
        } catch (Exception e) {
            writer.dispose();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }

    @Override
    public boolean replaceImage(AppReplaceImageRequest req, UserLoginVO userVO) {
        resourceAuthHelper.requireAppEditable(req.getAppId(), userVO);

        App app = appService.getById(req.getAppId());
        BusinessAssert.notNull(app, ErrorCode.NOT_FOUND, "应用不存在");

        String appCodeDir = buildAppCodeDir(app);
        String normalizedNewImagePath = normalizeWebImagePath(req.getNewImagePath());
        ensureImageExistsInAppDir(appCodeDir, normalizedNewImagePath);

        File indexFile = new File(appCodeDir, "index.html");
        BusinessAssert.requireTrue(indexFile.exists() && indexFile.isFile(),
                ErrorCode.NOT_FOUND, "未找到 index.html，无法替换图片");

        Document document;
        try {
            document = Jsoup.parse(indexFile, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取 HTML 文件失败");
        }

        Element target = locateTargetElement(document, req);
        BusinessAssert.notNull(target, ErrorCode.NOT_FOUND, "未定位到目标图片元素");

        boolean replaced = false;
        if ("img".equalsIgnoreCase(target.tagName())) {
            target.attr("src", normalizedNewImagePath);
            replaced = true;
        } else if ("picture".equalsIgnoreCase(target.tagName())) {
            Element img = target.selectFirst("img");
            if (img != null) {
                img.attr("src", normalizedNewImagePath);
                replaced = true;
            }
        } else {
            Element img = target.selectFirst("img");
            if (img != null) {
                img.attr("src", normalizedNewImagePath);
                replaced = true;
            }
        }

        BusinessAssert.requireTrue(replaced, ErrorCode.BUSINESS_ERROR, "目标元素不是可替换图片节点");

        FileUtil.writeString(document.outerHtml(), indexFile, StandardCharsets.UTF_8);
        log.info("app image replaced, appId={}, userId={}, newImagePath={}",
                req.getAppId(), userVO.getId(), normalizedNewImagePath);
        return true;
    }

    private String buildAppCodeDir(App app) {
        return appDeployConfig.getOutputDir() + File.separator + app.getCodeGenType() + "_" + app.getId();
    }

    private String normalizeImageExt(String originalFilename) {
        String ext = FileUtil.extName(originalFilename);
        BusinessAssert.notBlank(ext, ErrorCode.PARAMS_ERROR, "图片文件扩展名非法");
        return ext.trim().toLowerCase(Locale.ROOT);
    }

    private void validateImageFile(String contentType, String ext) {
        String normalizedMime = StrUtil.blankToDefault(contentType, "").trim().toLowerCase(Locale.ROOT);
        BusinessAssert.requireTrue(AppAssetConfig.ALLOWED_IMAGE_MIME.contains(normalizedMime),
                ErrorCode.PARAMS_ERROR, "仅支持 jpg / jpeg / png / gif / webp / svg 图片");
        BusinessAssert.requireTrue(AppAssetConfig.ALLOWED_IMAGE_EXT.contains(ext),
                ErrorCode.PARAMS_ERROR, "仅支持 jpg / jpeg / png / gif / webp / svg 图片");
    }

    private String normalizeWebImagePath(String rawPath) {
        String path = StrUtil.blankToDefault(rawPath, "").trim().replace("\\", "/");
        if (path.startsWith("/")) {
            path = StrUtil.removePrefix(path, "/");
        }
        BusinessAssert.requireTrue(path.startsWith(AppAssetConfig.IMAGE_WEB_ROOT_NO_SLASH + "/"),
                ErrorCode.PARAMS_ERROR,
                "图片路径必须以 " + AppAssetConfig.IMAGE_WEB_ROOT_NO_SLASH + "/ 开头");
        return path;
    }

    private void ensureImageExistsInAppDir(String appCodeDir, String webPath) {
        String relative = StrUtil.removePrefix(webPath, "/").replace("/", File.separator);
        Path root = Paths.get(appCodeDir).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        BusinessAssert.requireTrue(target.startsWith(root), ErrorCode.NO_PERMISSION, "图片路径非法");
        File targetFile = target.toFile();
        BusinessAssert.requireTrue(targetFile.exists() && targetFile.isFile(),
                ErrorCode.NOT_FOUND, "目标图片不存在，请先上传");
    }

    private void validateAppImageQuota(String appCodeDir, long incomingBytes) {
        File imgDir = new File(appCodeDir, AppAssetConfig.IMAGE_WEB_ROOT_NO_SLASH);
        if (!imgDir.exists() || !imgDir.isDirectory()) {
            return;
        }
        AtomicInteger count = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0L);
        FileUtil.loopFiles(imgDir, file -> file.isFile()).forEach(file -> {
            count.incrementAndGet();
            totalBytes.addAndGet(file.length());
        });
        BusinessAssert.requireTrue(count.get() < appAssetConfig.getMaxImageCountPerApp(),
                ErrorCode.BUSINESS_ERROR, "图片数量超过单应用上限");
        BusinessAssert.requireTrue(
                totalBytes.get() + incomingBytes <= appAssetConfig.getMaxTotalImageSizeBytesPerApp(),
                ErrorCode.BUSINESS_ERROR,
                "图片总大小超过单应用上限"
        );
    }

    private Element locateTargetElement(Document doc, AppReplaceImageRequest req) {
        Element byXpath = findByXpath(doc, req.getTargetXpath());
        if (byXpath != null) {
            return byXpath;
        }
        if (StrUtil.isNotBlank(req.getTargetId())) {
            Element byId = doc.getElementById(req.getTargetId());
            if (byId != null) {
                return byId;
            }
        }
        Element byTagPath = findByTagPath(doc, req.getTargetTagPath());
        if (byTagPath != null) {
            return byTagPath;
        }
        String tag = StrUtil.blankToDefault(req.getTargetTag(), "").trim().toLowerCase(Locale.ROOT);
        if (StrUtil.isNotBlank(tag)) {
            Elements byTag = doc.getElementsByTag(tag);
            if (!byTag.isEmpty()) {
                if (req.getTargetClassList() != null && !req.getTargetClassList().isEmpty()) {
                    for (Element candidate : byTag) {
                        if (containsAllClasses(candidate, req.getTargetClassList())) {
                            return candidate;
                        }
                    }
                }
                return byTag.first();
            }
        }
        return null;
    }

    private boolean containsAllClasses(Element candidate, List<String> classes) {
        for (String cls : classes) {
            if (StrUtil.isBlank(cls)) {
                continue;
            }
            if (!candidate.hasClass(cls.trim())) {
                return false;
            }
        }
        return true;
    }

    private Element findByXpath(Document doc, String xpath) {
        if (StrUtil.isBlank(xpath)) {
            return null;
        }
        try {
            Elements elements = doc.selectXpath(xpath);
            return elements.isEmpty() ? null : elements.first();
        } catch (Exception ignore) {
            return null;
        }
    }

    private Element findByTagPath(Document doc, List<String> tagPath) {
        if (tagPath == null || tagPath.isEmpty()) {
            return null;
        }
        Element current = doc;
        for (String rawTag : tagPath) {
            String tag = StrUtil.blankToDefault(rawTag, "").trim().toLowerCase(Locale.ROOT);
            if (StrUtil.isBlank(tag)) {
                continue;
            }
            Element next = null;
            for (Element child : current.children()) {
                if (tag.equalsIgnoreCase(child.tagName())) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                return null;
            }
            current = next;
        }
        return current == doc ? null : current;
    }
}
