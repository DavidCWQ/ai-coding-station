package com.cwq.project_aicodingstation.app.version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 应用代码目录里的版本快照。快照放在 {@code .versions/{messageId}}，拷贝时跳过这层目录本身。
 */
public final class CodeVersionFiles {

    public static final String DIR_NAME = ".versions";

    private static final Pattern CODE_FENCE = Pattern.compile(
            "```(?:html|css|js|javascript)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_HTML = Pattern.compile(
            "^\\s*(?:<!doctype|<html\\b)", Pattern.CASE_INSENSITIVE);

    private CodeVersionFiles() {
    }

    /**
     * 与预览刷新规则一致：html/css/js 代码块，或整段 HTML。
     */
    public static boolean hasSavableCode(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (CODE_FENCE.matcher(message).find()) {
            return true;
        }
        return RAW_HTML.matcher(message).find();
    }

    public static Path appDir(String outputDir, String codeGenType, Long appId) {
        return Path.of(outputDir, codeGenType + "_" + appId).toAbsolutePath().normalize();
    }

    public static Path versionsDir(Path appDir) {
        return appDir.resolve(DIR_NAME);
    }

    public static Path snapshotDir(Path appDir, long messageId) {
        return versionsDir(appDir).resolve(Long.toString(messageId));
    }

    public static boolean hasIndex(Path dir) {
        return dir != null && Files.isRegularFile(dir.resolve("index.html"));
    }

    /**
     * 用源目录的内容替换目标目录，不把 {@code .versions} 拷进去。
     */
    public static void replaceWithTreeExceptVersions(Path source, Path target) throws IOException {
        Path sourceDir = source.toAbsolutePath().normalize();
        Path targetDir = target.toAbsolutePath().normalize();
        if (Files.exists(targetDir)) {
            deleteRecursively(targetDir);
        }
        copyChildren(sourceDir, targetDir);
    }

    /**
     * 把快照内容铺回应用目录，保留 {@code .versions}。
     */
    public static void restoreSnapshot(Path appDir, Path snapshot) throws IOException {
        Path app = appDir.toAbsolutePath().normalize();
        Path snap = snapshot.toAbsolutePath().normalize();
        Files.createDirectories(app);
        Path parent = app.getParent() == null ? app : app.getParent();
        Path staging = Files.createTempDirectory(parent, "code-restore-");
        try {
            copyChildren(snap, staging);
            try (Stream<Path> children = Files.list(app)) {
                for (Path child : children.toList()) {
                    if (DIR_NAME.equals(child.getFileName().toString())) {
                        continue;
                    }
                    deleteRecursively(child);
                }
            }
            copyChildren(staging, app);
        } finally {
            deleteRecursively(staging);
        }
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            List<Path> all = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path item : all) {
                Files.deleteIfExists(item);
            }
        }
    }

    public static List<String> snapshotNames(Path appDir) throws IOException {
        Path versions = versionsDir(appDir);
        if (!Files.isDirectory(versions)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(versions)) {
            return children
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    private static void copyChildren(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        if (!Files.isDirectory(source)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.toList()) {
                Path relative = source.relativize(path);
                if (relative.getNameCount() == 0) {
                    continue;
                }
                if (DIR_NAME.equals(relative.getName(0).toString())) {
                    continue;
                }
                Path dest = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    if (dest.getParent() != null) {
                        Files.createDirectories(dest.getParent());
                    }
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
