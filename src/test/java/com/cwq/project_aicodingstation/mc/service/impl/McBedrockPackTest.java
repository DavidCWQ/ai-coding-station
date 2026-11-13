package com.cwq.project_aicodingstation.mc.service.impl;

import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.mc.config.McBedrockProperties;
import com.cwq.project_aicodingstation.mc.dto.McPackToggleRequest;
import com.cwq.project_aicodingstation.mc.vo.McDownload;
import com.cwq.project_aicodingstation.mc.vo.McPackVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McBedrockPackTest {

    private static final String RES = "11111111-1111-1111-1111-111111111111";
    private static final String BEH = "33333333-3333-3333-3333-333333333333";
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @TempDir
    Path home;

    private final ObjectMapper json = new ObjectMapper();
    private McBedrockServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        Path world = home.resolve("data").resolve("worlds").resolve("TestWorld");
        Files.createDirectories(world);
        Files.writeString(world.resolve("levelname.txt"), "Test World", StandardCharsets.UTF_8);
        Files.writeString(home.resolve("data").resolve("server.properties"), """
                server-name=My World v6.0
                level-name=TestWorld
                gamemode=survival
                """, StandardCharsets.UTF_8);
        McBedrockProperties properties = new McBedrockProperties();
        properties.setEnabled(true);
        properties.setHome(home.toString());
        properties.setExecScripts(false);
        service = new McBedrockServiceImpl(properties, json);
    }

    @Test
    void resourcePackShowsChineseCardAndEnablesOnCurrentWorld() throws Exception {
        service.uploadPack(file("星光.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8),
                "texts/zh_CN.lang", "pack.name=星光材质\npack.description=把草地换成星空\n".getBytes(StandardCharsets.UTF_8),
                "pack_icon.png", PNG
        )));
        McPackVO card = onlyPack();
        assertEquals("星光材质", card.getName());
        assertEquals("把草地换成星空", card.getDescription());
        assertEquals("resources", card.getKind());
        assertEquals("1.0.0", card.getVersion());
        assertEquals("Ada", card.getAuthors());
        assertEquals("1.21.50", card.getMinEngine());
        assertTrue(card.isHasIcon());
        assertFalse(card.getEnabledWorlds().contains("TestWorld"));
        assertTrue(service.visitorConsole().getPacks().isEmpty());
        assertThrows(BusinessException.class, () -> service.packIcon(card.getId(), false));

        toggle(card.getId(), "TestWorld", true);
        JsonNode listed = json.readTree(home.resolve("data/worlds/TestWorld/world_resource_packs.json").toFile());
        assertEquals(RES, listed.get(0).path("pack_id").asText());
        assertEquals(1, listed.get(0).path("version").get(0).asInt());
        assertTrue(service.visitorConsole().getPacks().stream().anyMatch(p -> "星光材质".equals(p.getName())));
        assertTrue(service.packIcon(card.getId(), false).exists());
        Properties props = new Properties();
        try (var in = Files.newInputStream(home.resolve("data/server.properties"))) {
            props.load(in);
        }
        assertEquals("true", props.getProperty("texturepack-required"));
        assertTrue(Files.readString(home.resolve(".env")).contains("TEXTUREPACK_REQUIRED=true"));

        toggle(card.getId(), "TestWorld", false);
        props.clear();
        try (var in = Files.newInputStream(home.resolve("data/server.properties"))) {
            props.load(in);
        }
        assertEquals("false", props.getProperty("texturepack-required"));
    }

    @Test
    void addonIsOneCardAndOtherWorldDoesNotForceTexturePack() throws Exception {
        Path other = home.resolve("data/worlds/Other");
        Files.createDirectories(other);
        service.uploadPack(file("星辉.mcaddon", Map.of(
                "bp/manifest.json", behaviorManifest(BEH, "[1, 0, 0]", false, null).getBytes(StandardCharsets.UTF_8),
                "bp/pack_icon.png", PNG,
                "rp/manifest.json", resourceManifest(RES, "[1, 2, 0]").getBytes(StandardCharsets.UTF_8),
                "rp/texts/zh_CN.lang", "pack.name=资源之名\npack.description=外观\n".getBytes(StandardCharsets.UTF_8)
        )));
        McPackVO card = onlyPack();
        assertEquals("addon", card.getKind());
        assertEquals("行为之名", card.getName());
        assertTrue(card.isHasIcon());
        assertEquals("行为 1.0.0 · 资源 1.2.0", card.getVersion());

        toggle(card.getId(), "Other", true);
        assertTrue(Files.isRegularFile(other.resolve("world_behavior_packs.json")));
        assertTrue(Files.isRegularFile(other.resolve("world_resource_packs.json")));
        assertFalse(Files.isRegularFile(home.resolve("data/worlds/TestWorld/world_resource_packs.json")));
        Properties props = new Properties();
        try (var in = Files.newInputStream(home.resolve("data/server.properties"))) {
            props.load(in);
        }
        assertFalse(Boolean.parseBoolean(props.getProperty("texturepack-required", "false")));
    }

    @Test
    void reuploadUpdatesEnabledVersionAndKeepsOneCard() throws Exception {
        service.uploadPack(file("a.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8)
        )));
        toggle(onlyPack().getId(), "TestWorld", true);
        service.uploadPack(file("a.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[2, 0, 0]").getBytes(StandardCharsets.UTF_8)
        )));
        assertEquals(1, service.adminConsole().getPacks().size());
        JsonNode listed = json.readTree(home.resolve("data/worlds/TestWorld/world_resource_packs.json").toFile());
        assertEquals(2, listed.get(0).path("version").get(0).asInt());
    }

    @Test
    void missingDependencyBlocksEnableUntilTheOtherPackArrives() throws Exception {
        service.uploadPack(file("beh.mcpack", Map.of(
                "manifest.json", behaviorManifest(BEH, "[1, 0, 0]", true, RES).getBytes(StandardCharsets.UTF_8)
        )));
        McPackVO card = onlyPack();
        assertEquals("behavior", card.getKind());
        assertTrue(card.isHasScript());
        assertTrue(card.isExperimentHint());
        assertTrue(card.isMissingDependency());
        String id = card.getId();
        assertThrows(BusinessException.class, () -> toggle(id, "TestWorld", true));
        assertFalse(Files.isRegularFile(home.resolve("data/worlds/TestWorld/world_behavior_packs.json")));

        service.uploadPack(file("res.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8)
        )));
        McPackVO behavior = service.adminConsole().getPacks().stream()
                .filter(p -> "behavior".equals(p.getKind()))
                .findFirst()
                .orElseThrow();
        assertFalse(behavior.isMissingDependency());
        toggle(behavior.getId(), "TestWorld", true);
        assertTrue(Files.isRegularFile(home.resolve("data/worlds/TestWorld/world_behavior_packs.json")));
        assertFalse(Files.isRegularFile(home.resolve("data/worlds/TestWorld/world_resource_packs.json")));
    }

    @Test
    void rejectsMarketplaceSkinSlipAndWrongExtension() throws Exception {
        BusinessException market = assertThrows(BusinessException.class, () -> service.uploadPack(file("shop.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8),
                "contents.json", "{}".getBytes(StandardCharsets.UTF_8)
        ))));
        assertTrue(market.getMessage().contains("市场"));

        BusinessException skin = assertThrows(BusinessException.class, () -> service.uploadPack(file("skin.mcpack", Map.of(
                "manifest.json", """
                        {"format_version":2,"header":{"name":"Skin","uuid":"%s","version":[1,0,0]},"modules":[{"type":"skin_pack","uuid":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","version":[1,0,0]}]}
                        """.formatted(RES).getBytes(StandardCharsets.UTF_8)
        ))));
        assertTrue(skin.getMessage().contains("皮肤包"));

        BusinessException slip = assertThrows(BusinessException.class, () -> service.uploadPack(file("bad.zip", Map.of(
                "../evil.txt", "nope".getBytes(StandardCharsets.UTF_8)
        ))));
        assertTrue(slip.getMessage().contains("路径"));
        assertFalse(Files.exists(home.resolve("evil.txt")));

        BusinessException jar = assertThrows(BusinessException.class,
                () -> service.uploadPack(new MockMultipartFile("file", "mod.jar", "application/java-archive", new byte[]{1})));
        assertTrue(jar.getMessage().contains("mcpack"));

        MockMultipartFile huge = new MockMultipartFile("file", "big.mcpack", "application/zip", new byte[]{1, 2, 3}) {
            @Override
            public long getSize() {
                return 25L * 1024 * 1024;
            }
        };
        assertTrue(assertThrows(BusinessException.class, () -> service.uploadPack(huge)).getMessage().contains("24MB"));
        assertTrue(service.adminConsole().getPacks().isEmpty());
    }

    @Test
    void runningWorldRestartsWhenEnabledAndBackupKeepsThePack() throws Exception {
        Files.createDirectories(home.resolve("ops"));
        Files.writeString(home.resolve("ops/runtime.json"),
                "{\"running\":true,\"phase\":\"RUNNING\",\"joinHint\":\"\",\"updatedAt\":\"2099-01-01T00:00:00Z\"}",
                StandardCharsets.UTF_8);
        service.uploadPack(file("a.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8),
                "pack_icon.png", PNG
        )));
        assertEquals("QUEUED", toggle(onlyPack().getId(), "TestWorld", true).getStatus());
        assertEquals("STARTING", service.adminConsole().getRunPhase());

        service.backupNow();
        Path zip = home.resolve("backups").resolve(service.adminConsole().getBackups().getFirst().getFilename());
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            boolean found = archive.stream().anyMatch(entry ->
                    entry.getName().replace('\\', '/').contains("resource_packs/")
                            && entry.getName().endsWith("manifest.json"));
            assertTrue(found);
        }

        String id = onlyPack().getId();
        assertEquals("DONE", service.deletePack(id).getStatus());
        assertTrue(service.adminConsole().getPacks().isEmpty());
        assertFalse(Files.isDirectory(home.resolve("data/resource_packs").resolve(RES)));
    }

    @Test
    void enginePacksStayOffTheShelfAndCannotBeDeleted() throws Exception {
        String vanilla = "0575c61f-a5da-4b7f-9961-ffda2908861e";
        Path behavior = home.resolve("data/behavior_packs/vanilla");
        Path chemistry = home.resolve("data/behavior_packs/chemistry");
        Path versioned = home.resolve("data/resource_packs/vanilla_1.26.60");
        Path editor = home.resolve("data/resource_packs/editor");
        Path experimental = home.resolve("data/behavior_packs/experimental_villager_trade");
        Path serverLibrary = home.resolve("data/behavior_packs/server_library");
        for (Path dir : new Path[]{behavior, chemistry, versioned, editor, experimental, serverLibrary}) {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("manifest.json"), resourceManifest(vanilla, "[0, 0, 1]"), StandardCharsets.UTF_8);
        }
        service.uploadPack(file("a.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8)
        )));

        assertEquals(1, service.adminConsole().getPacks().size());
        assertTrue(service.adminConsole().getPacks().stream().noneMatch(pack -> pack.getId().contains(vanilla)));
        assertThrows(BusinessException.class, () -> service.deletePack("u-" + vanilla));
        assertTrue(Files.isRegularFile(behavior.resolve("manifest.json")));

        assertEquals("DONE", service.deletePack(onlyPack().getId()).getStatus());
        assertTrue(Files.isRegularFile(behavior.resolve("manifest.json")));
        assertTrue(Files.isRegularFile(chemistry.resolve("manifest.json")));
        assertTrue(Files.isRegularFile(versioned.resolve("manifest.json")));
        assertTrue(Files.isRegularFile(editor.resolve("manifest.json")));
        assertTrue(Files.isRegularFile(experimental.resolve("manifest.json")));
        assertTrue(Files.isRegularFile(serverLibrary.resolve("manifest.json")));
    }

    @Test
    void looseFolderWithoutLibraryStillShowsACard() throws Exception {
        Path root = home.resolve("data/resource_packs").resolve("hand-placed");
        Files.createDirectories(root);
        Files.writeString(root.resolve("manifest.json"), resourceManifest(RES, "[1, 0, 0]"), StandardCharsets.UTF_8);
        McPackVO card = onlyPack();
        assertTrue(card.getId().startsWith("u-"));
        assertEquals("resources", card.getKind());
    }

    @Test
    void largeResourcePackWarnsAboutPhones() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8));
        entries.put("textures/bulk.bin", new byte[12 * 1024 * 1024 + 1]);
        service.uploadPack(file("big.mcpack", entries));
        assertTrue(onlyPack().isLargeForMobile());
    }

    @Test
    void downloadPackWritesMcpackAndMcaddon() throws Exception {
        service.uploadPack(file("星光.mcpack", Map.of(
                "manifest.json", resourceManifest(RES, "[1, 0, 0]").getBytes(StandardCharsets.UTF_8),
                "texts/zh_CN.lang", "pack.name=星光材质\npack.description=把草地换成星空\n".getBytes(StandardCharsets.UTF_8),
                "pack_icon.png", PNG
        )));
        McPackVO resource = onlyPack();
        McDownload resourceFile = service.downloadPack(resource.getId());
        assertEquals("星光材质.mcpack", resourceFile.filename());
        try (ZipFile zip = new ZipFile(resourceFile.resource().getFile())) {
            assertTrue(zip.getEntry("manifest.json") != null);
            assertTrue(zip.getEntry("texts/zh_CN.lang") != null);
            assertTrue(zip.getEntry("pack_icon.png") != null);
        }

        service.uploadPack(file("星辉.mcaddon", Map.of(
                "bp/manifest.json", behaviorManifest(BEH, "[1, 0, 0]", false, null).getBytes(StandardCharsets.UTF_8),
                "bp/pack_icon.png", PNG,
                "rp/manifest.json", resourceManifest("66666666-6666-6666-6666-666666666666", "[1, 2, 0]").getBytes(StandardCharsets.UTF_8),
                "rp/texts/zh_CN.lang", "pack.name=资源之名\npack.description=外观\n".getBytes(StandardCharsets.UTF_8)
        )));
        McPackVO addon = service.adminConsole().getPacks().stream()
                .filter(pack -> "addon".equals(pack.getKind()))
                .findFirst()
                .orElseThrow();
        McDownload addonFile = service.downloadPack(addon.getId());
        assertEquals("行为之名.mcaddon", addonFile.filename());
        try (ZipFile zip = new ZipFile(addonFile.resource().getFile())) {
            long manifests = zip.stream().filter(entry -> entry.getName().endsWith("manifest.json")).count();
            assertEquals(2, manifests);
            assertTrue(zip.getEntry("manifest.json") == null);
            assertTrue(zip.stream().anyMatch(entry -> entry.getName().startsWith(BEH + "/")));
            assertTrue(zip.stream().anyMatch(entry -> entry.getName().startsWith("66666666-6666-6666-6666-666666666666/")));
        }
    }

    private McPackVO onlyPack() {
        assertEquals(1, service.adminConsole().getPacks().size());
        return service.adminConsole().getPacks().getFirst();
    }

    private com.cwq.project_aicodingstation.mc.dto.McOpResultVO toggle(String id, String world, boolean enabled) {
        McPackToggleRequest req = new McPackToggleRequest();
        req.setId(id);
        req.setWorld(world);
        req.setEnabled(enabled);
        return service.setPackEnabled(req);
    }

    private static MockMultipartFile file(String name, Map<String, byte[]> entries) throws Exception {
        return new MockMultipartFile("file", name, "application/zip", zip(entries));
    }

    private static byte[] zip(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    private static String resourceManifest(String uuid, String version) {
        return """
                {
                  "format_version": 2,
                  "header": {
                    "name": "pack.name",
                    "description": "pack.description",
                    "uuid": "%s",
                    "version": %s,
                    "min_engine_version": [1, 21, 50]
                  },
                  "modules": [
                    {"type": "resources", "uuid": "22222222-2222-2222-2222-222222222222", "version": [1, 0, 0]}
                  ],
                  "metadata": {"authors": ["Ada"]}
                }
                """.formatted(uuid, version);
    }

    private static String behaviorManifest(String uuid, String version, boolean script, String dependency) {
        String modules = script
                ? """
                {"type":"data","uuid":"44444444-4444-4444-4444-444444444444","version":[1,0,0]},
                {"type":"script","uuid":"55555555-5555-5555-5555-555555555555","version":[1,0,0]}
                """
                : """
                {"type":"data","uuid":"44444444-4444-4444-4444-444444444444","version":[1,0,0]}
                """;
        String deps = dependency == null
                ? ""
                : ",\"dependencies\":[{\"uuid\":\"%s\",\"version\":[1,0,0]},{\"module_name\":\"@minecraft/server\",\"version\":\"1.14.0-beta\"}]"
                .formatted(dependency);
        if (script && dependency == null) {
            deps = ",\"dependencies\":[{\"module_name\":\"@minecraft/server\",\"version\":\"1.14.0-beta\"}]";
        }
        return """
                {
                  "format_version": 2,
                  "header": {"name": "行为之名", "description": "玩法", "uuid": "%s", "version": %s},
                  "modules": [%s]
                  %s
                }
                """.formatted(uuid, version, modules, deps);
    }
}
