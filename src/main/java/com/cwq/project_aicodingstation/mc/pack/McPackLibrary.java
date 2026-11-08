package com.cwq.project_aicodingstation.mc.pack;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.common.utils.BusinessAssert;
import com.cwq.project_aicodingstation.mc.config.McBedrockProperties;
import com.cwq.project_aicodingstation.mc.vo.McPackVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 基岩附加包库。文件放在 data/resource_packs 与 data/behavior_packs，
 * 每个世界用自己的 world_*_packs.json 决定启用哪些。
 * 专用服自带的 vanilla、chemistry、experimental、editor、server_* 目录不是附加包，不进列表，也不能删。
 */
@Slf4j
public class McPackLibrary {

    public static final long MAX_UPLOAD_BYTES = 24L * 1024 * 1024;
    static final long MAX_UNPACKED_BYTES = 96L * 1024 * 1024;
    static final long MOBILE_WARN_BYTES = 12L * 1024 * 1024;
    private static final long MAX_ICON_BYTES = 2L * 1024 * 1024;
    private static final int MAX_ENTRIES = 20_000;
    private static final Pattern UUID_RE = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern ID_RE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,80}");

    private final McBedrockProperties properties;
    private final ObjectMapper objectMapper;

    public McPackLibrary(McBedrockProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public record PackChange(String name, boolean changed) {
    }

    public List<McPackVO> list(boolean admin) {
        Disk disk = readDisk();
        List<McPackVO> result = new ArrayList<>();
        Set<String> claimed = new HashSet<>();
        List<LibEntry> entries = new ArrayList<>(disk.library);
        entries.sort(Comparator.comparingLong((LibEntry e) -> e.uploadedAt).reversed());
        for (LibEntry entry : entries) {
            List<Part> parts = partsFor(entry.uuids, disk);
            if (parts.isEmpty()) {
                continue;
            }
            for (Part part : parts) {
                claimed.add(part.uuid);
            }
            result.add(toVo(entry.id, parts, admin, disk));
        }
        List<String> loose = new ArrayList<>();
        loose.addAll(disk.behaviors.keySet());
        loose.addAll(disk.resources.keySet());
        loose.removeAll(claimed);
        loose = loose.stream().distinct().sorted().toList();
        for (String uuid : loose) {
            result.add(toVo("u-" + uuid, partsFor(List.of(uuid), disk), admin, disk));
        }
        return result;
    }

    public Path icon(String id, boolean admin, String currentWorld) {
        Disk disk = readDisk();
        Loaded loaded = require(id, disk);
        if (!admin) {
            BusinessAssert.requireTrue(enabledOn(loaded.parts, currentWorld),
                    ErrorCode.NO_PERMISSION, "访客只能查看当前世界启用的附加包");
        }
        Path icon = chooseIcon(loaded.parts);
        BusinessAssert.requireTrue(icon != null && Files.isRegularFile(icon),
                ErrorCode.NOT_FOUND, "这个附加包没有图标");
        return icon;
    }

    public PackChange install(MultipartFile file, String currentWorld) {
        BusinessAssert.requireTrue(file != null && !file.isEmpty(), ErrorCode.PARAMS_MISSING, "请选择附加包");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int slash = Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\'));
        if (slash >= 0) {
            original = original.substring(slash + 1);
        }
        String lower = original.toLowerCase(Locale.ROOT);
        BusinessAssert.requireTrue(lower.endsWith(".mcpack") || lower.endsWith(".mcaddon") || lower.endsWith(".zip"),
                ErrorCode.PARAMS_INVALID, "只接受 .mcpack、.mcaddon 或 .zip");
        BusinessAssert.requireTrue(file.getSize() <= MAX_UPLOAD_BYTES,
                ErrorCode.PARAMS_INVALID, "附加包不能超过 24MB");
        Path tmp = null;
        try {
            tmp = Files.createTempFile("mc-pack-upload-", ".zip");
            file.transferTo(tmp);
            return installZip(tmp, currentWorld);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.warn("install pack failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理附加包失败");
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // temp cleanup
                }
            }
        }
    }

    public PackChange setEnabled(String id, String worldFolder, boolean enabled) {
        Disk disk = readDisk();
        Loaded loaded = require(id, disk);
        if (enabled && loaded.missingDependency) {
            throw new BusinessException(ErrorCode.PARAMS_INVALID, "还缺依赖包，不能启用");
        }
        Path world = worldDir(worldFolder);
        boolean changed = false;
        try {
            if (enabled) {
                for (Part part : loaded.parts) {
                    String filename = "resources".equals(part.kind)
                            ? "world_resource_packs.json"
                            : "world_behavior_packs.json";
                    changed |= upsertPack(world.resolve(filename), part.uuid, part.version);
                }
            } else {
                Set<String> uuids = uuidsOf(loaded.parts);
                changed |= removePacks(world.resolve("world_resource_packs.json"), uuids);
                changed |= removePacks(world.resolve("world_behavior_packs.json"), uuids);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入世界附加包列表失败");
        }
        return new PackChange(loaded.name, changed);
    }

    public PackChange delete(String id, String currentWorld) {
        Disk disk = readDisk();
        Loaded loaded = require(id, disk);
        Set<String> uuids = uuidsOf(loaded.parts);
        boolean changedCurrent = false;
        try {
            for (Path world : worldDirs()) {
                boolean changed = false;
                changed |= removePacks(world.resolve("world_resource_packs.json"), uuids);
                changed |= removePacks(world.resolve("world_behavior_packs.json"), uuids);
                if (changed && world.getFileName().toString().equals(currentWorld)) {
                    changedCurrent = true;
                }
            }
            deleteFolders(dataDir().resolve("resource_packs"), uuids);
            deleteFolders(dataDir().resolve("behavior_packs"), uuids);
            List<LibEntry> entries = new ArrayList<>(disk.library);
            entries.removeIf(entry -> entry.id.equals(id) || entry.uuids.stream().anyMatch(uuids::contains));
            writeLibrary(entries);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除附加包失败");
        }
        return new PackChange(loaded.name, changedCurrent);
    }

    public boolean resourcePackActive(String worldFolder) {
        if (!StringUtils.hasText(worldFolder)) {
            return false;
        }
        Path world;
        try {
            world = worldDir(worldFolder);
        } catch (BusinessException e) {
            return false;
        }
        Set<String> ids = readPackIds(world.resolve("world_resource_packs.json"));
        if (ids.isEmpty()) {
            return false;
        }
        Map<String, Part> resources = scan(dataDir().resolve("resource_packs"), "resources");
        for (String packId : ids) {
            if (resources.containsKey(packId)) {
                return true;
            }
        }
        return false;
    }

    /* ================== install ================== */

    private PackChange installZip(Path zip, String currentWorld) throws IOException {
        Path extracted = Files.createTempDirectory("mc-pack-");
        try {
            Budget budget = new Budget();
            unzip(zip, extracted, budget);
            explodeNested(extracted, budget);
            List<Path> roots = findRoots(extracted);
            BusinessAssert.requireTrue(!roots.isEmpty(), ErrorCode.PARAMS_INVALID, "没有找到资源包或行为包");
            List<ParsedPack> parsed = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Path root : roots) {
                ParsedPack pack = parse(root);
                BusinessAssert.requireTrue(seen.add(pack.uuid), ErrorCode.PARAMS_INVALID, "压缩包里有重复的包");
                parsed.add(pack);
            }
            boolean referenced = referencedBy(currentWorld, parsed);
            for (ParsedPack pack : parsed) {
                if (pack.behavior) {
                    place(pack.root, dataDir().resolve("behavior_packs").resolve(pack.uuid));
                }
                if (pack.resources) {
                    place(pack.root, dataDir().resolve("resource_packs").resolve(pack.uuid));
                }
            }
            for (ParsedPack pack : parsed) {
                refreshVersion(pack);
            }
            remember(parsed);
            return new PackChange(displayName(parsed), referenced);
        } finally {
            deleteTreeQuiet(extracted);
        }
    }

    private boolean referencedBy(String currentWorld, List<ParsedPack> parsed) {
        if (!StringUtils.hasText(currentWorld)) {
            return false;
        }
        Path world;
        try {
            world = worldDir(currentWorld);
        } catch (BusinessException e) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        ids.addAll(readPackIds(world.resolve("world_resource_packs.json")));
        ids.addAll(readPackIds(world.resolve("world_behavior_packs.json")));
        for (ParsedPack pack : parsed) {
            if (ids.contains(pack.uuid)) {
                return true;
            }
        }
        return false;
    }

    private void refreshVersion(ParsedPack pack) throws IOException {
        for (Path world : worldDirs()) {
            if (pack.behavior) {
                touchVersion(world.resolve("world_behavior_packs.json"), pack.uuid, pack.version);
            }
            if (pack.resources) {
                touchVersion(world.resolve("world_resource_packs.json"), pack.uuid, pack.version);
            }
        }
    }

    private void remember(List<ParsedPack> parsed) throws IOException {
        LinkedHashSet<String> uploaded = new LinkedHashSet<>();
        for (ParsedPack pack : parsed) {
            if (pack.behavior) {
                uploaded.add(pack.uuid);
            }
        }
        for (ParsedPack pack : parsed) {
            uploaded.add(pack.uuid);
        }
        List<LibEntry> entries = readLibrary();
        LibEntry match = null;
        for (LibEntry entry : entries) {
            if (new HashSet<>(entry.uuids).equals(uploaded)) {
                match = entry;
                break;
            }
        }
        if (match == null) {
            for (LibEntry entry : entries) {
                entry.uuids.removeIf(uploaded::contains);
            }
            entries.removeIf(entry -> entry.uuids.isEmpty());
            match = new LibEntry();
            match.id = newId(entries);
            match.uuids = new ArrayList<>(uploaded);
            entries.add(match);
        } else {
            match.uuids = new ArrayList<>(uploaded);
            for (LibEntry entry : entries) {
                if (entry != match) {
                    entry.uuids.removeIf(uploaded::contains);
                }
            }
            entries.removeIf(entry -> entry.uuids.isEmpty());
        }
        match.uploadedAt = Instant.now().toEpochMilli();
        writeLibrary(entries);
    }

    private void explodeNested(Path extracted, Budget budget) throws IOException {
        List<Path> nested = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(extracted)) {
            for (Path path : walk.toList()) {
                if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)) {
                    continue;
                }
                String rel = extracted.relativize(path).toString().replace('\\', '/');
                if (rel.startsWith("_nested/") || rel.contains("__MACOSX")) {
                    continue;
                }
                int depth = rel.split("/").length - 1;
                if (depth > 1) {
                    continue;
                }
                String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (lower.endsWith(".mcpack") || lower.endsWith(".mcaddon") || lower.endsWith(".zip")) {
                    nested.add(path);
                }
            }
        }
        int index = 0;
        for (Path zip : nested) {
            Path dest = extracted.resolve("_nested").resolve(Integer.toString(index++));
            Files.createDirectories(dest);
            unzip(zip, dest, budget);
        }
    }

    private void unzip(Path zip, Path dest, Budget budget) throws IOException {
        Files.createDirectories(dest);
        Path root = dest.toAbsolutePath().normalize();
        try (InputStream in = Files.newInputStream(zip);
             ZipInputStream zin = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                budget.entries++;
                BusinessAssert.requireTrue(budget.entries <= MAX_ENTRIES,
                        ErrorCode.PARAMS_INVALID, "压缩包里的文件太多");
                String name = entry.getName().replace('\\', '/');
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.isBlank()) {
                    continue;
                }
                BusinessAssert.requireTrue(!name.contains("..") && !name.contains(":"),
                        ErrorCode.PARAMS_INVALID, "压缩包路径不合法");
                Path out = root.resolve(name).normalize();
                BusinessAssert.requireTrue(out.startsWith(root), ErrorCode.PARAMS_INVALID, "压缩包路径不合法");
                if (entry.isDirectory() || name.endsWith("/")) {
                    Files.createDirectories(out);
                    continue;
                }
                Files.createDirectories(out.getParent());
                try (OutputStream os = Files.newOutputStream(out)) {
                    long n = zin.transferTo(os);
                    budget.written += n;
                }
                BusinessAssert.requireTrue(budget.written <= MAX_UNPACKED_BYTES,
                        ErrorCode.PARAMS_INVALID, "解压后超过 96MB");
            }
        }
    }

    private List<Path> findRoots(Path extracted) throws IOException {
        List<Path> dirs = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(extracted)) {
            for (Path path : walk.toList()) {
                if (!Files.isRegularFile(path) || !"manifest.json".equals(path.getFileName().toString())) {
                    continue;
                }
                String rel = extracted.relativize(path).toString().replace('\\', '/');
                if (rel.contains("__MACOSX") || rel.contains("/.")) {
                    continue;
                }
                dirs.add(path.getParent());
            }
        }
        List<Path> roots = new ArrayList<>();
        for (Path dir : dirs) {
            boolean nested = false;
            Path cursor = dir.getParent();
            while (cursor != null && (cursor.equals(extracted) || cursor.startsWith(extracted))) {
                if (Files.isRegularFile(cursor.resolve("manifest.json"))) {
                    nested = true;
                    break;
                }
                if (cursor.equals(extracted)) {
                    break;
                }
                cursor = cursor.getParent();
            }
            if (!nested) {
                roots.add(dir);
            }
        }
        return roots;
    }

    private ParsedPack parse(Path root) throws IOException {
        BusinessAssert.requireTrue(!Files.isRegularFile(root.resolve("contents.json")),
                ErrorCode.PARAMS_INVALID, "这是市场加密包，专用服加载不了");
        Path manifest = root.resolve("manifest.json");
        BusinessAssert.requireTrue(Files.isRegularFile(manifest), ErrorCode.PARAMS_INVALID, "没有找到资源包或行为包");
        JsonNode tree;
        try {
            tree = objectMapper.readTree(readText(manifest));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_INVALID, "manifest.json 无法解析");
        }
        BusinessAssert.requireTrue(tree != null && tree.isObject(), ErrorCode.PARAMS_INVALID, "manifest.json 无法解析");
        JsonNode header = tree.path("header");
        String uuid = header.path("uuid").asText("").trim().toLowerCase(Locale.ROOT);
        BusinessAssert.requireTrue(UUID_RE.matcher(uuid).matches(), ErrorCode.PARAMS_INVALID, "包的 UUID 格式不对");
        JsonNode modules = tree.path("modules");
        BusinessAssert.requireTrue(modules.isArray() && !modules.isEmpty(),
                ErrorCode.PARAMS_INVALID, "manifest 里没有模块");
        boolean resources = false;
        boolean behavior = false;
        boolean script = false;
        boolean skin = false;
        boolean template = false;
        for (JsonNode module : modules) {
            String type = module.path("type").asText("").trim().toLowerCase(Locale.ROOT);
            switch (type) {
                case "resources", "client_data" -> resources = true;
                case "data" -> behavior = true;
                case "script" -> {
                    behavior = true;
                    script = true;
                }
                case "client_script" -> {
                    resources = true;
                    script = true;
                }
                case "skin_pack" -> skin = true;
                case "world_template" -> template = true;
                default -> {
                    // 未知模块不影响已经认出的资源包或行为包
                }
            }
        }
        if (!resources && !behavior) {
            if (skin) {
                throw new BusinessException(ErrorCode.PARAMS_INVALID, "皮肤包不能挂到专用服");
            }
            if (template) {
                throw new BusinessException(ErrorCode.PARAMS_INVALID, "世界模板请走存档上传");
            }
            throw new BusinessException(ErrorCode.PARAMS_INVALID, "不能识别的包类型");
        }
        ParsedPack pack = new ParsedPack();
        pack.uuid = uuid;
        pack.version = readVersion(header.get("version"));
        pack.minEngine = header.hasNonNull("min_engine_version") ? readVersion(header.get("min_engine_version")) : null;
        pack.resources = resources;
        pack.behavior = behavior;
        pack.hasScript = script;
        pack.root = root;
        Map<String, String> lang = loadLang(root);
        pack.name = clip(resolveText(header.path("name").asText(""), lang), 80);
        pack.description = clip(resolveText(header.path("description").asText(""), lang), 400);
        pack.authors = readAuthors(tree.path("metadata").path("authors"));
        readDependencies(tree.path("dependencies"), pack);
        return pack;
    }

    private void readDependencies(JsonNode node, ParsedPack pack) {
        if (!node.isArray()) {
            return;
        }
        for (JsonNode dep : node) {
            if (!dep.isObject()) {
                continue;
            }
            if (dep.hasNonNull("uuid")) {
                String uuid = dep.path("uuid").asText("").trim().toLowerCase(Locale.ROOT);
                if (UUID_RE.matcher(uuid).matches()) {
                    pack.depUuids.add(uuid);
                    pack.depVersions.add(dep.has("version") ? readVersion(dep.get("version")) : null);
                }
                continue;
            }
            String module = dep.path("module_name").asText("");
            String version = dep.path("version").isTextual() ? dep.path("version").asText("") : "";
            String haystack = (module + " " + version).toLowerCase(Locale.ROOT);
            if (haystack.contains("beta")) {
                pack.experimentHint = true;
            }
        }
    }

    private Map<String, String> loadLang(Path root) throws IOException {
        Map<String, String> lang = new LinkedHashMap<>();
        lang.putAll(readLang(findLang(root, "en_us")));
        lang.putAll(readLang(findLang(root, "zh_cn")));
        return lang;
    }

    private Path findLang(Path root, String language) throws IOException {
        Path texts = root.resolve("texts");
        if (!Files.isDirectory(texts)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(texts)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.equals(language) || name.equals(language + ".lang")) {
                    return file;
                }
            }
        }
        return null;
    }

    private Map<String, String> readLang(Path file) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        if (file == null) {
            return map;
        }
        for (String rawLine : readText(file).split("\n", -1)) {
            String line = rawLine.endsWith("\r") ? rawLine.substring(0, rawLine.length() - 1) : rawLine;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = unescape(line.substring(eq + 1).trim());
            if (!key.isEmpty() && !value.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static String resolveText(String raw, Map<String, String> lang) {
        if (raw == null) {
            return "";
        }
        String current = raw.trim();
        for (int i = 0; i < 2 && !current.isEmpty(); i++) {
            String next = lang.get(current);
            if (!StringUtils.hasText(next) || next.trim().equals(current)) {
                break;
            }
            current = next.trim();
        }
        return current;
    }

    private static String unescape(String raw) {
        return raw.replace("\\n", "\n").replace("\\t", " ").replace("\\\"", "\"");
    }

    private List<String> readAuthors(JsonNode node) {
        List<String> authors = new ArrayList<>();
        if (!node.isArray()) {
            return authors;
        }
        for (JsonNode item : node) {
            String name = item.isTextual() ? item.asText("") : item.path("name").asText("");
            name = name.trim();
            if (!name.isEmpty() && !authors.contains(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private void place(Path source, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        Path staging = dest.resolveSibling(dest.getFileName().toString() + ".incoming");
        deleteTree(staging);
        try {
            copyTree(source, staging);
            deleteTree(dest);
            moveReplacing(staging, dest);
        } catch (IOException e) {
            deleteTreeQuiet(staging);
            throw e;
        }
    }

    private static void moveReplacing(Path source, Path dest) throws IOException {
        try {
            Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomic) {
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyTree(Path source, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.toList()) {
                if (Files.isSymbolicLink(path)) {
                    continue;
                }
                Path rel = source.relativize(path);
                if (rel.toString().isEmpty()) {
                    continue;
                }
                String relUnix = rel.toString().replace('\\', '/');
                String name = path.getFileName().toString();
                if ("__MACOSX".equals(name) || ".DS_Store".equals(name) || "Thumbs.db".equals(name)
                        || relUnix.startsWith("__MACOSX/") || relUnix.contains("/__MACOSX/")) {
                    continue;
                }
                Path target = dest.resolve(rel);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /* ================== disk index ================== */

    private Disk readDisk() {
        Disk disk = new Disk();
        disk.behaviors = scan(dataDir().resolve("behavior_packs"), "behavior");
        disk.resources = scan(dataDir().resolve("resource_packs"), "resources");
        disk.library = readLibrary();
        return disk;
    }

    private Map<String, Part> scan(Path parent, String kind) {
        Map<String, Part> map = new LinkedHashMap<>();
        if (!Files.isDirectory(parent)) {
            return map;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
            List<Path> dirs = new ArrayList<>();
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    dirs.add(path);
                }
            }
            dirs.sort(Comparator.comparing(path -> path.getFileName().toString()));
            for (Path dir : dirs) {
                String folderName = dir.getFileName().toString();
                if (folderName.startsWith(".") || folderName.endsWith(".incoming") || enginePackFolder(folderName)) {
                    continue;
                }
                ParsedPack parsed = tryParse(dir);
                if (parsed == null) {
                    continue;
                }
                Part part = Part.from(parsed, kind, dir, dirSize(dir));
                Part previous = map.get(parsed.uuid);
                if (previous == null || folderName.equalsIgnoreCase(parsed.uuid)) {
                    map.put(parsed.uuid, part);
                }
            }
        } catch (IOException e) {
            log.warn("scan packs {} failed: {}", parent, e.getMessage());
        }
        return map;
    }

    private ParsedPack tryParse(Path root) {
        try {
            if (!Files.isRegularFile(root.resolve("manifest.json"))) {
                return null;
            }
            return parse(root);
        } catch (Exception e) {
            log.warn("skip pack {}: {}", root.getFileName(), e.getMessage());
            return null;
        }
    }

    private Loaded require(String id, Disk disk) {
        BusinessAssert.notBlank(id, ErrorCode.PARAMS_MISSING, "缺少附加包");
        BusinessAssert.requireTrue(ID_RE.matcher(id).matches(), ErrorCode.PARAMS_INVALID, "非法附加包");
        for (LibEntry entry : disk.library) {
            if (entry.id.equals(id)) {
                List<Part> parts = partsFor(entry.uuids, disk);
                BusinessAssert.requireTrue(!parts.isEmpty(), ErrorCode.NOT_FOUND, "没有这个附加包");
                return Loaded.of(parts, disk);
            }
        }
        if (id.startsWith("u-")) {
            String uuid = id.substring(2);
            if (UUID_RE.matcher(uuid).matches()) {
                boolean claimed = disk.library.stream().anyMatch(entry -> entry.uuids.contains(uuid));
                List<Part> parts = partsFor(List.of(uuid), disk);
                if (!claimed && !parts.isEmpty()) {
                    return Loaded.of(parts, disk);
                }
            }
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "没有这个附加包");
    }

    private List<Part> partsFor(List<String> uuids, Disk disk) {
        List<Part> parts = new ArrayList<>();
        for (String uuid : uuids) {
            Part behavior = disk.behaviors.get(uuid);
            if (behavior != null) {
                parts.add(behavior);
            }
            Part resources = disk.resources.get(uuid);
            if (resources != null) {
                parts.add(resources);
            }
        }
        return parts;
    }

    private McPackVO toVo(String id, List<Part> parts, boolean admin, Disk disk) {
        Loaded loaded = Loaded.of(parts, disk);
        Path icon = chooseIcon(parts);
        boolean hasIcon = icon != null;
        long resourceBytes = parts.stream()
                .filter(part -> "resources".equals(part.kind))
                .mapToLong(part -> part.sizeBytes)
                .sum();
        return McPackVO.builder()
                .id(id)
                .name(loaded.name)
                .description(loaded.description)
                .version(versionText(parts))
                .kind(kindOf(parts))
                .hasScript(parts.stream().anyMatch(part -> part.hasScript))
                .experimentHint(parts.stream().anyMatch(part -> part.experimentHint))
                .missingDependency(loaded.missingDependency)
                .dependencyNote(loaded.dependencyNote)
                .hasIcon(hasIcon)
                .iconUrl(hasIcon
                        ? (admin ? "/api/mc/admin/pack/icon?id=" : "/api/mc/pack/icon?id=") + url(id)
                        : null)
                .sizeBytes(parts.stream().mapToLong(part -> part.sizeBytes).sum())
                .largeForMobile(resourceBytes > MOBILE_WARN_BYTES)
                .authors(authorText(parts))
                .minEngine(minEngine(parts))
                .enabledWorlds(enabledWorlds(parts))
                .build();
    }

    private List<String> enabledWorlds(List<Part> parts) {
        List<String> names = new ArrayList<>();
        for (Path world : worldDirs()) {
            if (enabledOn(parts, world)) {
                names.add(world.getFileName().toString());
            }
        }
        return names;
    }

    private boolean enabledOn(List<Part> parts, String worldFolder) {
        if (!StringUtils.hasText(worldFolder) || parts.isEmpty()) {
            return false;
        }
        try {
            return enabledOn(parts, worldDir(worldFolder));
        } catch (BusinessException e) {
            return false;
        }
    }

    private boolean enabledOn(List<Part> parts, Path world) {
        Set<String> resources = readPackIds(world.resolve("world_resource_packs.json"));
        Set<String> behaviors = readPackIds(world.resolve("world_behavior_packs.json"));
        for (Part part : parts) {
            Set<String> bag = "resources".equals(part.kind) ? resources : behaviors;
            if (!bag.contains(part.uuid)) {
                return false;
            }
        }
        return true;
    }

    private static String kindOf(List<Part> parts) {
        boolean resources = parts.stream().anyMatch(part -> "resources".equals(part.kind));
        boolean behavior = parts.stream().anyMatch(part -> "behavior".equals(part.kind));
        if (resources && behavior) {
            return "addon";
        }
        return behavior ? "behavior" : "resources";
    }

    private static String versionText(List<Part> parts) {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        for (Part part : parts) {
            versions.add(fmt(part.version));
        }
        if (versions.size() <= 1) {
            return versions.isEmpty() ? "" : versions.iterator().next();
        }
        List<String> labeled = new ArrayList<>();
        for (Part part : parts) {
            labeled.add(("behavior".equals(part.kind) ? "行为 " : "资源 ") + fmt(part.version));
        }
        return String.join(" · ", labeled);
    }

    private static String authorText(List<Part> parts) {
        LinkedHashSet<String> authors = new LinkedHashSet<>();
        for (Part part : parts) {
            authors.addAll(part.authors);
        }
        List<String> list = new ArrayList<>(authors);
        if (list.isEmpty()) {
            return "";
        }
        if (list.size() > 3) {
            return String.join("、", list.subList(0, 3)) + " 等";
        }
        return String.join("、", list);
    }

    private static String minEngine(List<Part> parts) {
        int[] best = null;
        for (Part part : parts) {
            if (part.minEngine == null) {
                continue;
            }
            if (best == null || cmp(part.minEngine, best) > 0) {
                best = part.minEngine;
            }
        }
        if (best == null) {
            return "";
        }
        return fmt(best);
    }

    private static String displayName(List<ParsedPack> parsed) {
        for (ParsedPack pack : parsed) {
            if (pack.behavior && StringUtils.hasText(pack.name)) {
                return pack.name;
            }
        }
        for (ParsedPack pack : parsed) {
            if (StringUtils.hasText(pack.name)) {
                return pack.name;
            }
        }
        return "未命名附加包";
    }

    private Path chooseIcon(List<Part> parts) {
        List<Part> ordered = new ArrayList<>();
        for (Part part : parts) {
            if ("behavior".equals(part.kind)) {
                ordered.add(part);
            }
        }
        for (Part part : parts) {
            if ("resources".equals(part.kind)) {
                ordered.add(part);
            }
        }
        for (Part part : ordered) {
            Path icon = findIcon(part.root);
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }

    private Path findIcon(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return null;
        }
        Path fallback = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file) || Files.size(file) > MAX_ICON_BYTES) {
                    continue;
                }
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if ("pack_icon.png".equals(name)) {
                    return file;
                }
                if (("pack_icon.jpg".equals(name) || "pack_icon.jpeg".equals(name)) && fallback == null) {
                    fallback = file;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return fallback;
    }

    /* ================== world json ================== */

    private boolean upsertPack(Path file, String uuid, int[] version) throws IOException {
        ArrayNode arr = readArray(file);
        boolean found = false;
        boolean changed = false;
        for (JsonNode node : arr) {
            if (!node.isObject()) {
                continue;
            }
            String packId = node.path("pack_id").asText("").trim().toLowerCase(Locale.ROOT);
            if (!uuid.equals(packId)) {
                continue;
            }
            found = true;
            if (!Arrays.equals(readVersion(node.get("version")), version)) {
                ObjectNode row = (ObjectNode) node;
                row.put("pack_id", uuid);
                row.set("version", versionNode(version));
                changed = true;
            }
        }
        if (!found) {
            ObjectNode row = arr.addObject();
            row.put("pack_id", uuid);
            row.set("version", versionNode(version));
            changed = true;
        }
        if (changed) {
            writeJson(file, arr);
        }
        return changed;
    }

    private boolean touchVersion(Path file, String uuid, int[] version) throws IOException {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        ArrayNode arr = readArray(file);
        boolean changed = false;
        for (JsonNode node : arr) {
            if (!node.isObject()) {
                continue;
            }
            String packId = node.path("pack_id").asText("").trim().toLowerCase(Locale.ROOT);
            if (!uuid.equals(packId)) {
                continue;
            }
            if (!Arrays.equals(readVersion(node.get("version")), version)) {
                ObjectNode row = (ObjectNode) node;
                row.put("pack_id", uuid);
                row.set("version", versionNode(version));
                changed = true;
            }
        }
        if (changed) {
            writeJson(file, arr);
        }
        return changed;
    }

    private boolean removePacks(Path file, Set<String> uuids) throws IOException {
        if (!Files.isRegularFile(file) || uuids.isEmpty()) {
            return false;
        }
        ArrayNode arr = readArray(file);
        ArrayNode next = objectMapper.createArrayNode();
        boolean changed = false;
        for (JsonNode node : arr) {
            String packId = node.path("pack_id").asText("").trim().toLowerCase(Locale.ROOT);
            if (uuids.contains(packId)) {
                changed = true;
                continue;
            }
            next.add(node);
        }
        if (changed) {
            writeJson(file, next);
        }
        return changed;
    }

    private Set<String> readPackIds(Path file) {
        Set<String> ids = new LinkedHashSet<>();
        if (!Files.isRegularFile(file)) {
            return ids;
        }
        try {
            for (JsonNode node : readArray(file)) {
                String packId = node.path("pack_id").asText("").trim().toLowerCase(Locale.ROOT);
                if (!packId.isBlank()) {
                    ids.add(packId);
                }
            }
        } catch (IOException e) {
            log.warn("read {} failed: {}", file.getFileName(), e.getMessage());
        }
        return ids;
    }

    private ArrayNode readArray(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return objectMapper.createArrayNode();
        }
        JsonNode node = objectMapper.readTree(stripBom(Files.readAllBytes(file)));
        if (node != null && node.isArray()) {
            return (ArrayNode) node;
        }
        return objectMapper.createArrayNode();
    }

    private void writeJson(Path file, JsonNode node) throws IOException {
        Files.createDirectories(file.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), node);
    }

    private ArrayNode versionNode(int[] version) {
        ArrayNode node = objectMapper.createArrayNode();
        node.add(version[0]);
        node.add(version[1]);
        node.add(version[2]);
        return node;
    }

    /* ================== library file ================== */

    private List<LibEntry> readLibrary() {
        Path file = dataDir().resolve("pack-library.json");
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            JsonNode packs = root.path("packs");
            if (!packs.isArray()) {
                return new ArrayList<>();
            }
            List<LibEntry> entries = new ArrayList<>();
            for (JsonNode node : packs) {
                LibEntry entry = new LibEntry();
                entry.id = node.path("id").asText("");
                entry.uploadedAt = node.path("uploadedAt").asLong(0);
                if (node.path("uuids").isArray()) {
                    for (JsonNode uuidNode : node.path("uuids")) {
                        String uuid = uuidNode.asText("").trim().toLowerCase(Locale.ROOT);
                        if (UUID_RE.matcher(uuid).matches() && !entry.uuids.contains(uuid)) {
                            entry.uuids.add(uuid);
                        }
                    }
                }
                if (StringUtils.hasText(entry.id) && !entry.uuids.isEmpty()) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (IOException e) {
            log.warn("read pack library failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void writeLibrary(List<LibEntry> entries) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode packs = root.putArray("packs");
        for (LibEntry entry : entries) {
            ObjectNode node = packs.addObject();
            node.put("id", entry.id);
            node.put("uploadedAt", entry.uploadedAt);
            ArrayNode uuids = node.putArray("uuids");
            for (String uuid : entry.uuids) {
                uuids.add(uuid);
            }
        }
        Path file = dataDir().resolve("pack-library.json");
        Path tmp = dataDir().resolve("pack-library.json.tmp");
        writeJson(tmp, root);
        moveReplacing(tmp, file);
    }

    private static String newId(List<LibEntry> entries) {
        Set<String> used = new HashSet<>();
        for (LibEntry entry : entries) {
            used.add(entry.id);
        }
        for (int i = 0; i < 8; i++) {
            String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            if (!used.contains(id)) {
                return id;
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /* ================== paths ================== */

    private Path dataDir() {
        Path home = properties.homePath();
        BusinessAssert.requireTrue(home != null, ErrorCode.BUSINESS_ERROR, "基岩服目录未配置或不可访问");
        return home.resolve("data");
    }

    private Path worldDir(String folder) {
        BusinessAssert.notBlank(folder, ErrorCode.PARAMS_MISSING, "缺少存档");
        BusinessAssert.requireTrue(!folder.contains("..") && !folder.contains("/") && !folder.contains("\\"),
                ErrorCode.PARAMS_INVALID, "非法存档名");
        Path worlds = dataDir().resolve("worlds").normalize();
        Path world = worlds.resolve(folder).normalize();
        BusinessAssert.requireTrue(world.startsWith(worlds), ErrorCode.PARAMS_INVALID, "非法存档名");
        BusinessAssert.requireTrue(Files.isDirectory(world), ErrorCode.NOT_FOUND, "世界不存在");
        return world;
    }

    private List<Path> worldDirs() {
        Path worlds = dataDir().resolve("worlds");
        if (!Files.isDirectory(worlds)) {
            return List.of();
        }
        List<Path> dirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worlds)) {
            for (Path path : stream) {
                if (Files.isDirectory(path) && !path.getFileName().toString().startsWith(".")) {
                    dirs.add(path);
                }
            }
        } catch (IOException e) {
            log.warn("list worlds for packs failed: {}", e.getMessage());
        }
        return dirs;
    }

    private void deleteFolders(Path parent, Set<String> uuids) throws IOException {
        if (!Files.isDirectory(parent) || uuids.isEmpty()) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                String name = dir.getFileName().toString();
                if (name.startsWith(".") || name.endsWith(".incoming") || enginePackFolder(name)) {
                    continue;
                }
                if (uuids.contains(name.toLowerCase(Locale.ROOT))) {
                    deleteTree(dir);
                    continue;
                }
                ParsedPack parsed = tryParse(dir);
                if (parsed != null && uuids.contains(parsed.uuid)) {
                    deleteTree(dir);
                }
            }
        }
    }

    private void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        List<Path> all;
        try (Stream<Path> walk = Files.walk(root)) {
            all = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : all) {
            Files.deleteIfExists(path);
        }
    }

    private void deleteTreeQuiet(Path root) {
        try {
            deleteTree(root);
        } catch (IOException e) {
            log.warn("cleanup {} failed: {}", root, e.getMessage());
        }
    }

    /**
     * 专用服解压出来的官方包。界面上常显示成 pack.name，也没有写进世界的启用列表，
     * 但下次冷启动仍然要读，删掉会在打开世界时崩溃。
     */
    static boolean enginePackFolder(String folderName) {
        String name = folderName.toLowerCase(Locale.ROOT);
        return name.equals("vanilla")
                || name.startsWith("vanilla_")
                || name.equals("chemistry")
                || name.startsWith("chemistry_")
                || name.startsWith("experimental_")
                || name.equals("editor")
                || name.startsWith("editor_")
                || name.startsWith("server_");
    }

    private static long dirSize(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0L;
                }
            }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String readText(Path file) throws IOException {
        return new String(stripBom(Files.readAllBytes(file)), StandardCharsets.UTF_8);
    }

    private static byte[] stripBom(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return Arrays.copyOfRange(bytes, 3, bytes.length);
        }
        return bytes;
    }

    private static int[] readVersion(JsonNode node) {
        int[] version = new int[3];
        if (node == null || node.isNull()) {
            return version;
        }
        if (node.isArray()) {
            for (int i = 0; i < 3 && i < node.size(); i++) {
                version[i] = node.get(i).asInt(0);
            }
            return version;
        }
        if (node.isTextual()) {
            int index = 0;
            for (String bit : node.asText("").split("[^0-9]+")) {
                if (bit.isEmpty() || index >= 3) {
                    continue;
                }
                try {
                    version[index++] = Integer.parseInt(bit);
                } catch (NumberFormatException ignored) {
                    version[index++] = 0;
                }
            }
        }
        return version;
    }

    private static int cmp(int[] left, int[] right) {
        for (int i = 0; i < 3; i++) {
            int diff = left[i] - right[i];
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static String fmt(int[] version) {
        return version[0] + "." + version[1] + "." + version[2];
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "…";
    }

    private static String url(String id) {
        return java.net.URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    private static Set<String> uuidsOf(List<Part> parts) {
        Set<String> uuids = new LinkedHashSet<>();
        for (Part part : parts) {
            uuids.add(part.uuid);
        }
        return uuids;
    }

    private static final class Budget {
        private long written;
        private int entries;
    }

    private static final class Disk {
        private Map<String, Part> behaviors = new LinkedHashMap<>();
        private Map<String, Part> resources = new LinkedHashMap<>();
        private List<LibEntry> library = new ArrayList<>();
    }

    private static final class LibEntry {
        private String id;
        private long uploadedAt;
        private List<String> uuids = new ArrayList<>();
    }

    private static final class ParsedPack {
        private String uuid;
        private int[] version;
        private int[] minEngine;
        private boolean resources;
        private boolean behavior;
        private boolean hasScript;
        private boolean experimentHint;
        private final List<String> depUuids = new ArrayList<>();
        private final List<int[]> depVersions = new ArrayList<>();
        private String name = "";
        private String description = "";
        private List<String> authors = new ArrayList<>();
        private Path root;
    }

    private static final class Part {
        private String uuid;
        private String kind;
        private int[] version;
        private int[] minEngine;
        private boolean hasScript;
        private boolean experimentHint;
        private List<String> depUuids = List.of();
        private List<int[]> depVersions = List.of();
        private String name = "";
        private String description = "";
        private List<String> authors = List.of();
        private Path root;
        private long sizeBytes;

        private static Part from(ParsedPack parsed, String kind, Path root, long sizeBytes) {
            Part part = new Part();
            part.uuid = parsed.uuid;
            part.kind = kind;
            part.version = parsed.version;
            part.minEngine = parsed.minEngine;
            part.hasScript = parsed.hasScript;
            part.experimentHint = parsed.experimentHint;
            part.depUuids = new ArrayList<>(parsed.depUuids);
            part.depVersions = new ArrayList<>(parsed.depVersions);
            part.name = parsed.name;
            part.description = parsed.description;
            part.authors = new ArrayList<>(parsed.authors);
            part.root = root;
            part.sizeBytes = sizeBytes;
            return part;
        }
    }

    private static final class Loaded {
        private final List<Part> parts;
        private final String name;
        private final String description;
        private final boolean missingDependency;
        private final String dependencyNote;

        private Loaded(List<Part> parts, String name, String description,
                       boolean missingDependency, String dependencyNote) {
            this.parts = parts;
            this.name = name;
            this.description = description;
            this.missingDependency = missingDependency;
            this.dependencyNote = dependencyNote;
        }

        private static Loaded of(List<Part> parts, Disk disk) {
            String name = "";
            String description = "";
            for (Part part : parts) {
                if ("behavior".equals(part.kind) && StringUtils.hasText(part.name)) {
                    name = part.name;
                    description = part.description;
                    break;
                }
            }
            if (!StringUtils.hasText(name)) {
                for (Part part : parts) {
                    if (StringUtils.hasText(part.name)) {
                        name = part.name;
                        description = part.description;
                        break;
                    }
                }
            }
            if (!StringUtils.hasText(name)) {
                name = "未命名附加包";
            }
            int missing = 0;
            int older = 0;
            for (Part part : parts) {
                for (int i = 0; i < part.depUuids.size(); i++) {
                    String uuid = part.depUuids.get(i);
                    if (parts.stream().anyMatch(self -> self.uuid.equals(uuid))) {
                        continue;
                    }
                    int[] installed = installedVersion(uuid, disk);
                    if (installed == null) {
                        missing++;
                        continue;
                    }
                    int[] required = i < part.depVersions.size() ? part.depVersions.get(i) : null;
                    if (required != null && cmp(installed, required) < 0) {
                        older++;
                    }
                }
            }
            String note = "";
            if (missing > 0) {
                note = missing == 1 ? "缺少依赖包" : "缺少 " + missing + " 个依赖包";
            } else if (older > 0) {
                note = "依赖的版本低于要求";
            }
            return new Loaded(parts, name, description, missing > 0, note);
        }

        private static int[] installedVersion(String uuid, Disk disk) {
            Part behavior = disk.behaviors.get(uuid);
            if (behavior != null) {
                return behavior.version;
            }
            Part resources = disk.resources.get(uuid);
            if (resources != null) {
                return resources.version;
            }
            return null;
        }
    }
}
