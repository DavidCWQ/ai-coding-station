package com.cwq.project_aicodingstation.app.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    @Value("${ai.code.output-dir}")
    private String previewRootDir;

    /**
     * 提供静态资源访问，支持目录重定向与默认 index.html。
     * 访问格式: /api/static/{codeGenType}_{appId}[/{fileName}]
     */
    @GetMapping("/{codeDir:[a-zA-Z0-9_-]+}/**")
    public ResponseEntity<Resource> serveStaticResource(@PathVariable String codeDir,
                                                        HttpServletRequest request) {
        try {
            // 1. 获取资源路径
            String fullPath = request.getRequestURI();
            String prefix = request.getContextPath() + "/static/" + codeDir;
            String resourcePath = fullPath.substring(prefix.length());

            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.LOCATION, fullPath + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            if ("/".equals(resourcePath)) { // 默认返回 index.html
                resourcePath = "/index.html";
            }

            // 2. 构建文件系统路径
            Path root = Paths.get(previewRootDir).toAbsolutePath().normalize();
            String relative = (codeDir + resourcePath).replace("\\", "/");
            Path target = root.resolve(relative).normalize();

            // 3. 安全检查（防止路径穿越）
            if (!target.startsWith(root)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 4. 检查文件是否存在
            File file = target.toFile();
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            // 5. 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, getContentTypeWithCharset(file.getName()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getContentTypeWithCharset(String fileName) {
        String lower = StringUtils.trimAllWhitespace(fileName).toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        return "application/octet-stream";
    }
}
