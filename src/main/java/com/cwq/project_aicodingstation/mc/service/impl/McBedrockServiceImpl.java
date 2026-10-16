package com.cwq.project_aicodingstation.mc.service.impl;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.mc.catalog.McGameruleCatalog;
import com.cwq.project_aicodingstation.mc.config.McBedrockProperties;
import com.cwq.project_aicodingstation.mc.dto.McAllowlistUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McGameruleUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McOpResultVO;
import com.cwq.project_aicodingstation.mc.dto.McPermissionUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McSettingsUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McWorldActivateRequest;
import com.cwq.project_aicodingstation.mc.service.McBedrockService;
import com.cwq.project_aicodingstation.mc.vo.McBackupVO;
import com.cwq.project_aicodingstation.mc.vo.McConsoleVO;
import com.cwq.project_aicodingstation.mc.vo.McGameruleVO;
import com.cwq.project_aicodingstation.mc.vo.McPlayerVO;
import com.cwq.project_aicodingstation.mc.vo.McSettingsVO;
import com.cwq.project_aicodingstation.mc.vo.McWorldVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class McBedrockServiceImpl implements McBedrockService {

    private final McBedrockProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public McConsoleVO visitorConsole() {
        return buildConsole(false);
    }

    @Override
    public McConsoleVO adminConsole() {
        return buildConsole(true);
    }

    @Override
    public Resource worldIcon(String folder, boolean admin) {
        requireConfigured();
        Path world = resolveWorldDir(folder);
        if (!admin) {
            BusinessAssert.requireTrue(folder.equals(currentLevelName()),
                    ErrorCode.NO_PERMISSION, "访客只能查看当前世界");
        }
        Path icon = world.resolve("world_icon.jpeg");
        if (!Files.isRegularFile(icon)) {
            icon = world.resolve("world_icon.jpg");
        }
        BusinessAssert.requireTrue(Files.isRegularFile(icon), ErrorCode.NOT_FOUND, "该世界还没有图标");
        return new FileSystemResource(icon);
    }

    @Override
    public Resource downloadWorld(String folder) {
        requireConfigured();
        Path world = resolveWorldDir(folder);
        Path zip;
        try {
            zip = Files.createTempFile("mc-world-", ".zip");
            zip.toFile().deleteOnExit();
            zipWorld(world, zip);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打包世界失败");
        }
        return new FileSystemResource(zip);
    }

    @Override
    public McOpResultVO updateSettings(McSettingsUpdateRequest req) {
        requireConfigured();
        Properties props = loadServerProperties();
        if (StringUtils.hasText(req.getServerName())) {
            props.setProperty("server-name", req.getServerName().trim());
        }
        if (StringUtils.hasText(req.getGamemode())) {
            props.setProperty("gamemode", req.getGamemode().trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(req.getDifficulty())) {
            props.setProperty("difficulty", req.getDifficulty().trim().toLowerCase(Locale.ROOT));
        }
        if (req.getMaxPlayers() != null) {
            BusinessAssert.requireTrue(req.getMaxPlayers() >= 1 && req.getMaxPlayers() <= 8,
                    ErrorCode.PARAMS_INVALID, "人数上限建议 1–8");
            props.setProperty("max-players", String.valueOf(req.getMaxPlayers()));
        }
        if (req.getViewDistance() != null) {
            BusinessAssert.requireTrue(req.getViewDistance() >= 5 && req.getViewDistance() <= 16,
                    ErrorCode.PARAMS_INVALID, "视距需在 5–16");
            props.setProperty("view-distance", String.valueOf(req.getViewDistance()));
        }
        if (req.getTickDistance() != null) {
            BusinessAssert.requireTrue(req.getTickDistance() >= 4 && req.getTickDistance() <= 12,
                    ErrorCode.PARAMS_INVALID, "模拟距离需在 4–12");
            props.setProperty("tick-distance", String.valueOf(req.getTickDistance()));
        }
        if (req.getPlayerIdleTimeout() != null) {
            props.setProperty("player-idle-timeout", String.valueOf(Math.max(0, req.getPlayerIdleTimeout())));
        }
        if (req.getAllowList() != null) {
            props.setProperty("allow-list", String.valueOf(req.getAllowList()));
        }
        storeServerProperties(props);
        syncEnv(Map.of(
                "SERVER_NAME", props.getProperty("server-name", ""),
                "GAMEMODE", props.getProperty("gamemode", "survival"),
                "DIFFICULTY", props.getProperty("difficulty", "normal"),
                "MAX_PLAYERS", props.getProperty("max-players", "2"),
                "VIEW_DISTANCE", props.getProperty("view-distance", "8"),
                "TICK_DISTANCE", props.getProperty("tick-distance", "4"),
                "PLAYER_IDLE_TIMEOUT", props.getProperty("player-idle-timeout", "60"),
                "ALLOW_LIST", props.getProperty("allow-list", "false")
        ));
        return enqueue("RESTART", Map.of("reason", "settings"));
    }

    @Override
    public McOpResultVO updateGamerules(McGameruleUpdateRequest req) {
        requireConfigured();
        BusinessAssert.notNull(req.getRules(), ErrorCode.PARAMS_MISSING, "规则为空");
        Properties stored = loadManagedGamerules();
        List<String> commands = new ArrayList<>();
        req.getRules().forEach((id, value) -> {
            BusinessAssert.requireTrue(McGameruleCatalog.isAllowed(id),
                    ErrorCode.PARAMS_INVALID, "不允许的规则: " + id);
            stored.setProperty(id, String.valueOf(Boolean.TRUE.equals(value)));
            commands.add("gamerule " + id + " " + Boolean.TRUE.equals(value));
        });
        storeManagedGamerules(stored);
        return enqueue("APPLY_GAMERULES", Map.of("commands", commands));
    }

    @Override
    public McOpResultVO updatePermission(McPermissionUpdateRequest req) {
        requireConfigured();
        BusinessAssert.notBlank(req.getXuid(), ErrorCode.PARAMS_MISSING, "缺少 XUID");
        String perm = normalizePermission(req.getPermission());
        try {
            Path file = dataDir().resolve("permissions.json");
            ArrayNode arr = readJsonArray(file);
            boolean found = false;
            for (JsonNode n : arr) {
                if (n.isObject() && req.getXuid().equals(n.path("xuid").asText())) {
                    ((ObjectNode) n).put("permission", perm);
                    if (StringUtils.hasText(req.getName())) {
                        ((ObjectNode) n).put("name", req.getName().trim());
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                ObjectNode row = objectMapper.createObjectNode();
                row.put("permission", perm);
                row.put("xuid", req.getXuid().trim());
                if (StringUtils.hasText(req.getName())) {
                    row.put("name", req.getName().trim());
                }
                arr.add(row);
            }
            writeJson(file, arr);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入 permissions.json 失败");
        }
        return enqueue("RELOAD_PERMISSIONS", Map.of("xuid", req.getXuid(), "permission", perm));
    }

    @Override
    public McOpResultVO updateAllowlist(McAllowlistUpdateRequest req) {
        requireConfigured();
        BusinessAssert.notBlank(req.getXuid(), ErrorCode.PARAMS_MISSING, "缺少 XUID");
        try {
            Path file = dataDir().resolve("allowlist.json");
            ArrayNode arr = readJsonArray(file);
            ArrayNode next = objectMapper.createArrayNode();
            boolean found = false;
            for (JsonNode n : arr) {
                if (!n.isObject()) {
                    continue;
                }
                if (req.getXuid().equals(n.path("xuid").asText())) {
                    found = true;
                    if (req.isAllowlisted()) {
                        ObjectNode row = (ObjectNode) n;
                        if (StringUtils.hasText(req.getName())) {
                            row.put("name", req.getName().trim());
                        }
                        next.add(row);
                    }
                } else {
                    next.add(n);
                }
            }
            if (req.isAllowlisted() && !found) {
                ObjectNode row = objectMapper.createObjectNode();
                row.put("ignoresPlayerLimit", false);
                row.put("xuid", req.getXuid().trim());
                row.put("name", StringUtils.hasText(req.getName()) ? req.getName().trim() : req.getXuid());
                next.add(row);
            }
            writeJson(file, next);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入 allowlist.json 失败");
        }
        return enqueue("RELOAD_ALLOWLIST", Map.of("xuid", req.getXuid()));
    }

    @Override
    public McOpResultVO activateWorld(McWorldActivateRequest req) {
        requireConfigured();
        BusinessAssert.notBlank(req.getFolder(), ErrorCode.PARAMS_MISSING, "缺少世界文件夹");
        resolveWorldDir(req.getFolder());
        syncEnv(Map.of("LEVEL_NAME", req.getFolder()));
        Properties props = loadServerProperties();
        props.setProperty("level-name", req.getFolder());
        storeServerProperties(props);
        writeRuntimePhase("STARTING", false, "正在切换存档并启动");
        return enqueue("ACTIVATE", Map.of("folder", req.getFolder()));
    }

    @Override
    public McOpResultVO backupNow() {
        requireConfigured();
        Path backups = home().resolve("backups");
        String stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(java.time.ZoneId.systemDefault())
                .format(Instant.now());
        Path zip = backups.resolve("worlds-" + stamp + ".zip");
        try {
            Files.createDirectories(backups);
            zipWorldsSnapshot(zip);
            cloneCurrentWorld(stamp);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成备份失败");
        }
        writeRuntimePhase("BACKING_UP", readRuntime().running(), "正在冷备");
        enqueue("BACKUP", Map.of("alsoCold", true));
        return McOpResultVO.builder()
                .opId("inline")
                .action("BACKUP")
                .status("DONE")
                .message("已在存档里加了一张卡片，备份文件也写好了")
                .build();
    }

    @Override
    public McOpResultVO deleteWorld(String folder) {
        requireConfigured();
        Path world = resolveWorldDir(folder);
        BusinessAssert.requireTrue(!sanitizeFolder(folder).equals(currentLevelName()),
                ErrorCode.PARAMS_INVALID, "不能删除正在运行的世界，请先切换到别的存档");
        try {
            deleteTree(world);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除存档失败");
        }
        return McOpResultVO.builder()
                .opId("inline")
                .action("DELETE_WORLD")
                .status("DONE")
                .message("已删除存档")
                .build();
    }

    @Override
    public McOpResultVO deleteBackup(String filename) {
        requireConfigured();
        Path file = resolveBackupFile(filename);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除备份失败");
        }
        return McOpResultVO.builder()
                .opId("inline")
                .action("DELETE_BACKUP")
                .status("DONE")
                .message("已删除备份")
                .build();
    }

    @Override
    public McOpResultVO startServer() {
        requireConfigured();
        writeRuntimePhase("STARTING", false, "正在启动，通常 10–30 秒");
        return enqueue("START", Map.of());
    }

    @Override
    public McOpResultVO stopServer() {
        requireConfigured();
        writeRuntimePhase("STOPPING", readRuntime().running(), "正在停止");
        return enqueue("STOP", Map.of());
    }

    @Override
    public McOpResultVO opStatus(String opId) {
        requireConfigured();
        Path out = opsDir().resolve("outbox").resolve(opId + ".json");
        Path in = opsDir().resolve("inbox").resolve(opId + ".json");
        try {
            if (Files.isRegularFile(out)) {
                return objectMapper.readValue(out.toFile(), McOpResultVO.class);
            }
            if (Files.isRegularFile(in)) {
                return McOpResultVO.builder().opId(opId).status("QUEUED").action("").message("等待宿主机执行").build();
            }
        } catch (IOException e) {
            log.warn("read op status failed: {}", e.getMessage());
        }
        return McOpResultVO.builder().opId(opId).status("UNKNOWN").message("找不到该任务").build();
    }

    /* ================== console ================== */

    private McConsoleVO buildConsole(boolean admin) {
        if (!isConfigured()) {
            return McConsoleVO.builder()
                    .configured(false)
                    .viewerRole(admin ? "ADMIN" : "VISITOR")
                    .message("尚未配置 mc.bedrock.home。在服务器 .env 设置 MC_BEDROCK_HOST_DIR 后，把目录挂进后端即可。")
                    .worlds(List.of())
                    .gamerules(List.of())
                    .players(List.of())
                    .backups(List.of())
                    .build();
        }
        String current = currentLevelName();
        List<McWorldVO> worlds = listWorlds(current, admin);
        McWorldVO currentWorld = worlds.stream()
                .filter(McWorldVO::isCurrent)
                .findFirst()
                .orElse(worlds.stream().findFirst().orElse(null));
        List<McBackupVO> backups = admin ? listBackups() : List.of();
        Long lastBackup = backups.stream().map(McBackupVO::getCreatedEpochMs).max(Long::compareTo).orElse(null);
        RuntimeStatus runtime = readRuntime();
        return McConsoleVO.builder()
                .configured(true)
                .viewerRole(admin ? "ADMIN" : "VISITOR")
                .message(admin ? "可以切换存档、改设置、立即备份。" : "你当前是访客，只能查看正在运行的世界。")
                .running(runtime.running())
                .runPhase(runtime.phase())
                .serverName(loadServerProperties().getProperty("server-name", "My World v6.0"))
                .joinHint(runtime.joinHint())
                .currentWorld(currentWorld)
                .worlds(admin ? worlds : (currentWorld == null ? List.of() : List.of(currentWorld)))
                .settings(readSettings())
                .gamerules(readGamerules())
                .players(readPlayers())
                .backups(backups)
                .lastBackupEpochMs(lastBackup)
                .build();
    }

    private List<McWorldVO> listWorlds(String current, boolean admin) {
        Path worlds = dataDir().resolve("worlds");
        if (!Files.isDirectory(worlds)) {
            return List.of();
        }
        List<McWorldVO> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worlds)) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir) || dir.getFileName().toString().startsWith(".")) {
                    continue;
                }
                String folder = dir.getFileName().toString();
                String display = readDisplayName(dir, folder);
                boolean isCurrent = folder.equals(current);
                Path icon = Files.isRegularFile(dir.resolve("world_icon.jpeg"))
                        ? dir.resolve("world_icon.jpeg")
                        : dir.resolve("world_icon.jpg");
                boolean hasIcon = Files.isRegularFile(icon);
                String iconUrl = hasIcon
                        ? (admin
                        ? "/api/mc/admin/world/icon?folder=" + url(folder)
                        : "/api/mc/world/icon?folder=" + url(folder))
                        : null;
                result.add(McWorldVO.builder()
                        .folder(folder)
                        .displayName(display)
                        .current(isCurrent)
                        .sizeBytes(dirSize(dir))
                        .lastModifiedEpochMs(Files.getLastModifiedTime(dir).toMillis())
                        .hasIcon(hasIcon)
                        .iconUrl(iconUrl)
                        .build());
            }
        } catch (IOException e) {
            log.warn("list worlds failed: {}", e.getMessage());
        }
        result.sort(Comparator.comparing(McWorldVO::isCurrent).reversed()
                .thenComparing(McWorldVO::getLastModifiedEpochMs, Comparator.reverseOrder()));
        return result;
    }

    private McSettingsVO readSettings() {
        Properties p = loadServerProperties();
        return McSettingsVO.builder()
                .serverName(p.getProperty("server-name", "My World v6.0"))
                .levelName(p.getProperty("level-name", currentLevelName()))
                .gamemode(p.getProperty("gamemode", "survival"))
                .difficulty(p.getProperty("difficulty", "normal"))
                .maxPlayers(parseInt(p.getProperty("max-players"), 2))
                .viewDistance(parseInt(p.getProperty("view-distance"), 8))
                .tickDistance(parseInt(p.getProperty("tick-distance"), 4))
                .playerIdleTimeout(parseInt(p.getProperty("player-idle-timeout"), 60))
                .allowList(Boolean.parseBoolean(p.getProperty("allow-list", "false")))
                .allowCheats(Boolean.parseBoolean(p.getProperty("allow-cheats", "false")))
                .onlineMode(Boolean.parseBoolean(p.getProperty("online-mode", "true")))
                .build();
    }

    private List<McGameruleVO> readGamerules() {
        Properties stored = loadManagedGamerules();
        List<McGameruleVO> list = new ArrayList<>();
        for (McGameruleCatalog.Def def : McGameruleCatalog.NON_CHEAT) {
            String raw = stored.getProperty(def.id());
            boolean value = raw == null ? def.defaultOn() : Boolean.parseBoolean(raw);
            list.add(McGameruleVO.builder()
                    .id(def.id())
                    .label(def.label())
                    .hint(def.hint())
                    .hot(def.hot())
                    .value(value)
                    .build());
        }
        return list;
    }

    private List<McPlayerVO> readPlayers() {
        Map<String, McPlayerVO> byXuid = new LinkedHashMap<>();
        try {
            ArrayNode perms = readJsonArray(dataDir().resolve("permissions.json"));
            for (JsonNode n : perms) {
                if (!n.isObject()) {
                    continue;
                }
                String xuid = n.path("xuid").asText("");
                if (xuid.isBlank()) {
                    continue;
                }
                byXuid.put(xuid, McPlayerVO.builder()
                        .xuid(xuid)
                        .name(n.path("name").asText(""))
                        .permission(n.path("permission").asText("member"))
                        .allowlisted(false)
                        .build());
            }
            ArrayNode allow = readJsonArray(dataDir().resolve("allowlist.json"));
            for (JsonNode n : allow) {
                if (!n.isObject()) {
                    continue;
                }
                String xuid = n.path("xuid").asText("");
                if (xuid.isBlank()) {
                    continue;
                }
                McPlayerVO existing = byXuid.get(xuid);
                if (existing == null) {
                    byXuid.put(xuid, McPlayerVO.builder()
                            .xuid(xuid)
                            .name(n.path("name").asText(""))
                            .permission("member")
                            .allowlisted(true)
                            .build());
                } else {
                    existing.setAllowlisted(true);
                    if (!StringUtils.hasText(existing.getName()) && StringUtils.hasText(n.path("name").asText())) {
                        existing.setName(n.path("name").asText());
                    }
                }
            }
        } catch (IOException e) {
            log.warn("read players failed: {}", e.getMessage());
        }
        return new ArrayList<>(byXuid.values());
    }

    private List<McBackupVO> listBackups() {
        Path dir = home().resolve("backups");
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<McBackupVO> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "worlds-*")) {
            for (Path f : stream) {
                if (!Files.isRegularFile(f)) {
                    continue;
                }
                list.add(McBackupVO.builder()
                        .filename(f.getFileName().toString())
                        .sizeBytes(Files.size(f))
                        .createdEpochMs(Files.getLastModifiedTime(f).toMillis())
                        .build());
            }
        } catch (IOException e) {
            log.warn("list backups failed: {}", e.getMessage());
        }
        list.sort(Comparator.comparing(McBackupVO::getCreatedEpochMs).reversed());
        return list;
    }

    /* ================== ops ================== */

    private McOpResultVO enqueue(String action, Map<String, Object> args) {
        String opId = UUID.randomUUID().toString().replace("-", "");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("opId", opId);
        node.put("action", action);
        node.put("createdAt", Instant.now().toString());
        node.set("args", objectMapper.valueToTree(args));
        try {
            Path inbox = opsDir().resolve("inbox");
            Files.createDirectories(inbox);
            Files.createDirectories(opsDir().resolve("outbox"));
            Path file = inbox.resolve(opId + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), node);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入运维队列失败");
        }
        if (properties.isExecScripts()) {
            tryRunLocal(action, opId, args);
        }
        return McOpResultVO.builder()
                .opId(opId)
                .action(action)
                .status("QUEUED")
                .message("已提交：" + action + "。宿主机 ops-worker 或本机脚本将执行。")
                .build();
    }

    private void tryRunLocal(String action, String opId, Map<String, Object> args) {
        Path script = switch (action) {
            case "BACKUP" -> home().resolve("scripts").resolve("backup-worlds.sh");
            case "START", "STOP", "RESTART", "ACTIVATE" -> home().resolve("scripts").resolve("ops-worker.sh");
            case "APPLY_GAMERULES" -> home().resolve("scripts").resolve("send-cmd.sh");
            default -> null;
        };
        if (script == null || !Files.isRegularFile(script)) {
            return;
        }
        // 真正执行交给 ops-worker 一次消费，避免这里和 cron 抢锁
        log.info("queued bedrock op {} {} (worker will pick up)", action, opId);
    }

    /* ================== files ================== */

    private boolean isConfigured() {
        return properties.isEnabled() && properties.homePath() != null && Files.isDirectory(properties.homePath());
    }

    private void requireConfigured() {
        BusinessAssert.requireTrue(isConfigured(), ErrorCode.BUSINESS_ERROR, "基岩服目录未配置或不可访问");
    }

    private Path home() {
        return properties.homePath();
    }

    private Path dataDir() {
        return home().resolve("data");
    }

    private Path opsDir() {
        return home().resolve("ops");
    }

    private Path resolveWorldDir(String folder) {
        String safe = sanitizeFolder(folder);
        Path world = dataDir().resolve("worlds").resolve(safe).normalize();
        BusinessAssert.requireTrue(world.startsWith(dataDir().resolve("worlds").normalize()),
                ErrorCode.PARAMS_INVALID, "非法世界路径");
        BusinessAssert.requireTrue(Files.isDirectory(world), ErrorCode.NOT_FOUND, "世界不存在");
        return world;
    }

    private Path resolveBackupFile(String filename) {
        BusinessAssert.notBlank(filename, ErrorCode.PARAMS_MISSING, "备份名为空");
        BusinessAssert.requireTrue(
                filename.startsWith("worlds-")
                        && !filename.contains("..")
                        && !filename.contains("/")
                        && !filename.contains("\\"),
                ErrorCode.PARAMS_INVALID, "非法备份名");
        Path file = home().resolve("backups").resolve(filename).normalize();
        BusinessAssert.requireTrue(file.startsWith(home().resolve("backups").normalize()),
                ErrorCode.PARAMS_INVALID, "非法备份路径");
        BusinessAssert.requireTrue(Files.isRegularFile(file), ErrorCode.NOT_FOUND, "备份不存在");
        return file;
    }

    private static void deleteTree(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除文件失败");
                }
            });
        }
    }

    static String sanitizeFolder(String folder) {
        BusinessAssert.notBlank(folder, ErrorCode.PARAMS_MISSING, "世界名为空");
        BusinessAssert.requireTrue(!folder.contains("..") && !folder.contains("/") && !folder.contains("\\"),
                ErrorCode.PARAMS_INVALID, "非法世界名");
        return folder;
    }

    private String currentLevelName() {
        String fromProps = loadServerProperties().getProperty("level-name", "");
        if (StringUtils.hasText(fromProps)) {
            return fromProps;
        }
        return readEnv("LEVEL_NAME", "");
    }

    private String readDisplayName(Path world, String fallback) {
        Path txt = world.resolve("levelname.txt");
        if (Files.isRegularFile(txt)) {
            try {
                String name = Files.readString(txt, StandardCharsets.UTF_8).trim();
                if (!name.isBlank()) {
                    return name;
                }
            } catch (IOException ignored) {
                // fall through
            }
        }
        return fallback;
    }

    private Properties loadServerProperties() {
        Properties p = new Properties();
        Path file = dataDir().resolve("server.properties");
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                log.warn("load server.properties failed: {}", e.getMessage());
            }
        }
        return p;
    }

    private void storeServerProperties(Properties p) {
        Path file = dataDir().resolve("server.properties");
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "updated by ai-coding-station mc admin");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入 server.properties 失败");
        }
    }

    private Properties loadManagedGamerules() {
        Properties p = new Properties();
        Path file = dataDir().resolve("managed-gamerules.properties");
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                log.warn("load gamerules failed: {}", e.getMessage());
            }
        }
        return p;
    }

    private void storeManagedGamerules(Properties p) {
        Path file = dataDir().resolve("managed-gamerules.properties");
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "non-cheat gamerules managed by the website");
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入 gamerules 失败");
        }
    }

    private void syncEnv(Map<String, String> updates) {
        Path env = home().resolve(".env");
        List<String> lines = new ArrayList<>();
        if (Files.isRegularFile(env)) {
            try {
                lines.addAll(Files.readAllLines(env, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取 .env 失败");
            }
        }
        for (Map.Entry<String, String> e : updates.entrySet()) {
            boolean replaced = false;
            String line = e.getKey() + "=" + e.getValue();
            for (int i = 0; i < lines.size(); i++) {
                String raw = lines.get(i);
                if (raw.startsWith(e.getKey() + "=")) {
                    lines.set(i, line);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lines.add(line);
            }
        }
        try {
            Files.write(env, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入 .env 失败");
        }
    }

    private String readEnv(String key, String fallback) {
        Path env = home().resolve(".env");
        if (!Files.isRegularFile(env)) {
            return fallback;
        }
        try {
            for (String line : Files.readAllLines(env, StandardCharsets.UTF_8)) {
                if (line.startsWith(key + "=")) {
                    return line.substring(key.length() + 1).trim();
                }
            }
        } catch (IOException ignored) {
            return fallback;
        }
        return fallback;
    }

    private ArrayNode readJsonArray(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return objectMapper.createArrayNode();
        }
        JsonNode node = objectMapper.readTree(file.toFile());
        if (node != null && node.isArray()) {
            return (ArrayNode) node;
        }
        return objectMapper.createArrayNode();
    }

    private void writeJson(Path file, JsonNode node) throws IOException {
        Files.createDirectories(file.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), node);
    }

    private RuntimeStatus readRuntime() {
        boolean running = false;
        String phase = "SLEEPING";
        String joinHint = "";
        Path file = opsDir().resolve("runtime.json");
        if (Files.isRegularFile(file)) {
            try {
                JsonNode n = objectMapper.readTree(file.toFile());
                running = n.path("running").asBoolean(false);
                joinHint = n.path("joinHint").asText("");
                phase = n.path("phase").asText(running ? "RUNNING" : "SLEEPING");
            } catch (IOException e) {
                log.warn("read runtime failed: {}", e.getMessage());
            }
        }
        if (running) {
            phase = "RUNNING";
        }
        String pending = pendingInboxAction();
        if (!running && ("START".equals(pending) || "ACTIVATE".equals(pending) || "RESTART".equals(pending))) {
            phase = "STARTING";
        } else if ("STOP".equals(pending)) {
            phase = "STOPPING";
        } else if ("BACKUP".equals(pending)) {
            phase = "BACKING_UP";
        }
        return new RuntimeStatus(running, phase, joinHint);
    }

    private String pendingInboxAction() {
        Path inbox = opsDir().resolve("inbox");
        if (!Files.isDirectory(inbox)) {
            return "";
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inbox, "*.json")) {
            for (Path f : stream) {
                try {
                    String action = objectMapper.readTree(f.toFile()).path("action").asText("");
                    if ("START".equals(action) || "STOP".equals(action) || "ACTIVATE".equals(action)
                            || "RESTART".equals(action) || "BACKUP".equals(action)) {
                        return action;
                    }
                } catch (IOException ignored) {
                    // skip a broken ticket
                }
            }
        } catch (IOException e) {
            log.warn("scan inbox failed: {}", e.getMessage());
        }
        return "";
    }

    private void writeRuntimePhase(String phase, boolean running, String joinHint) {
        try {
            RuntimeStatus prev = readRuntime();
            ObjectNode n = objectMapper.createObjectNode();
            n.put("running", running);
            n.put("phase", phase);
            n.put("joinHint", StringUtils.hasText(joinHint) ? joinHint : prev.joinHint());
            n.put("updatedAt", Instant.now().toString());
            writeJson(opsDir().resolve("runtime.json"), n);
        } catch (IOException e) {
            log.warn("write runtime failed: {}", e.getMessage());
        }
    }

    private void cloneCurrentWorld(String stamp) throws IOException {
        String current = currentLevelName();
        if (!StringUtils.hasText(current)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "还没有当前世界，无法冷备");
        }
        Path src = dataDir().resolve("worlds").resolve(current);
        if (!Files.isDirectory(src)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前世界文件夹不存在");
        }
        Path dest = uniqueCloneDir(current, stamp);
        copyWorldTree(src, dest);
        String label = readDisplayName(src, current) + " · " + stamp.substring(4, 6) + "-"
                + stamp.substring(6, 8) + " " + stamp.substring(9, 11) + ":" + stamp.substring(11, 13);
        Files.writeString(dest.resolve("levelname.txt"), label, StandardCharsets.UTF_8);
    }

    private Path uniqueCloneDir(String current, String stamp) {
        Path worlds = dataDir().resolve("worlds");
        String base = current + "-" + stamp;
        Path dest = worlds.resolve(base);
        int i = 2;
        while (Files.exists(dest)) {
            dest = worlds.resolve(base + "-" + i++);
        }
        return dest;
    }

    private void copyWorldTree(Path src, Path dest) throws IOException {
        try (Stream<Path> walk = Files.walk(src)) {
            walk.forEach(p -> {
                String name = p.getFileName().toString();
                if ("LOCK".equalsIgnoreCase(name) || name.endsWith(".lock")) {
                    return;
                }
                Path target = dest.resolve(src.relativize(p));
                try {
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.warn("skip copy {}: {}", p, e.getMessage());
                }
            });
        }
    }

    private void zipWorldsSnapshot(Path zip) throws IOException {
        Path data = dataDir();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            Path worlds = data.resolve("worlds");
            if (Files.isDirectory(worlds)) {
                try (Stream<Path> walk = Files.walk(worlds)) {
                    walk.filter(Files::isRegularFile).forEach(p -> putZip(zos, data, p));
                }
            }
            for (String name : List.of("server.properties", "allowlist.json", "permissions.json")) {
                Path f = data.resolve(name);
                if (Files.isRegularFile(f)) {
                    putZip(zos, data, f);
                }
            }
        }
    }

    private static void putZip(ZipOutputStream zos, Path root, Path file) {
        try {
            zos.putNextEntry(new ZipEntry(root.relativize(file).toString().replace('\\', '/')));
            Files.copy(file, zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入备份压缩包失败");
        }
    }

    private void zipWorld(Path world, Path zip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip));
             Stream<Path> walk = Files.walk(world)) {
            Path root = world.getFileName();
            walk.filter(Files::isRegularFile).forEach(p -> {
                try {
                    Path rel = root.resolve(world.relativize(p));
                    zos.putNextEntry(new ZipEntry(rel.toString().replace('\\', '/')));
                    Files.copy(p, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打包世界文件失败");
                }
            });
        }
    }

    private static long dirSize(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0L;
                }
            }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return raw == null ? fallback : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String normalizePermission(String raw) {
        String p = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if ("operator".equals(p) || "op".equals(p)) {
            return "operator";
        }
        if ("visitor".equals(p)) {
            return "visitor";
        }
        return "member";
    }

    private static String url(String folder) {
        return java.net.URLEncoder.encode(folder, StandardCharsets.UTF_8);
    }

    private record RuntimeStatus(boolean running, String phase, String joinHint) {}
}
