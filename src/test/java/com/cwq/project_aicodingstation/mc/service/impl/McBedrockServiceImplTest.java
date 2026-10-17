package com.cwq.project_aicodingstation.mc.service.impl;

import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.mc.config.McBedrockProperties;
import com.cwq.project_aicodingstation.mc.dto.McAllowlistUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McGameruleUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McPermissionUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McSettingsUpdateRequest;
import com.cwq.project_aicodingstation.mc.dto.McWorldActivateRequest;
import com.cwq.project_aicodingstation.mc.vo.McConsoleVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McBedrockServiceImplTest {

    @TempDir
    Path home;

    private McBedrockServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        Path data = home.resolve("data");
        Path world = data.resolve("worlds").resolve("TestWorld");
        Files.createDirectories(world);
        Files.writeString(world.resolve("levelname.txt"), "Test World", StandardCharsets.UTF_8);
        Files.writeString(world.resolve("level.dat"), "stub", StandardCharsets.UTF_8);
        Files.writeString(data.resolve("server.properties"), """
                server-name=My World v6.0
                level-name=TestWorld
                gamemode=survival
                difficulty=normal
                max-players=2
                view-distance=8
                tick-distance=6
                player-idle-timeout=60
                allow-list=false
                """, StandardCharsets.UTF_8);
        McBedrockProperties properties = new McBedrockProperties();
        properties.setEnabled(true);
        properties.setHome(home.toAbsolutePath().toString());
        properties.setExecScripts(false);
        service = new McBedrockServiceImpl(properties, new ObjectMapper());
    }

    @Test
    void consoleListsWorldAndSleepsWhenNoRuntime() {
        McConsoleVO admin = service.adminConsole();
        assertTrue(admin.isConfigured());
        assertEquals("ADMIN", admin.getViewerRole());
        assertFalse(admin.isRunning());
        assertEquals("SLEEPING", admin.getRunPhase());
        assertEquals(1, admin.getWorlds().size());
        assertEquals("Test World", admin.getWorlds().getFirst().getDisplayName());
        assertTrue(admin.getGamerules().stream().anyMatch(r -> "pvp".equals(r.getId()) && !r.getValue()));

        McConsoleVO visitor = service.visitorConsole();
        assertEquals("VISITOR", visitor.getViewerRole());
        assertEquals(1, visitor.getWorlds().size());
        assertTrue(visitor.getBackups().isEmpty());
    }

    @Test
    void disabledHomeReturnsUnconfiguredConsole() {
        McBedrockProperties properties = new McBedrockProperties();
        properties.setEnabled(false);
        McBedrockServiceImpl disabled = new McBedrockServiceImpl(properties, new ObjectMapper());
        McConsoleVO vo = disabled.adminConsole();
        assertFalse(vo.isConfigured());
        assertTrue(vo.getWorlds().isEmpty());
    }

    @Test
    void backupCreatesWorldCardAndZipThenDeleteWorks() {
        service.backupNow();
        McConsoleVO after = service.adminConsole();
        assertEquals(2, after.getWorlds().size());
        assertFalse(after.getBackups().isEmpty());
        String zip = after.getBackups().getFirst().getFilename();
        assertTrue(zip.startsWith("worlds-"));
        assertTrue(Files.isRegularFile(home.resolve("backups").resolve(zip)));

        String clone = after.getWorlds().stream()
                .filter(w -> !w.isCurrent())
                .findFirst()
                .orElseThrow()
                .getFolder();
        assertEquals("DONE", service.deleteWorld(clone).getStatus());
        assertEquals("DONE", service.deleteBackup(zip).getStatus());
        assertEquals(1, service.adminConsole().getWorlds().size());
        assertTrue(service.adminConsole().getBackups().isEmpty());
    }

    @Test
    void deleteWorldRejectsCurrentFolder() {
        assertThrows(BusinessException.class, () -> service.deleteWorld("TestWorld"));
        assertThrows(BusinessException.class, () -> service.deleteWorld("../etc"));
    }

    @Test
    void startWritesStartingPhase() {
        assertEquals("QUEUED", service.startServer().getStatus());
        assertEquals("STARTING", service.adminConsole().getRunPhase());
    }

    @Test
    void stopWritesStoppingPhase() {
        assertEquals("QUEUED", service.stopServer().getStatus());
        assertEquals("STOPPING", service.adminConsole().getRunPhase());
    }

    @Test
    void settingsGamerulesPlayersAndActivate() {
        McSettingsUpdateRequest settings = new McSettingsUpdateRequest();
        settings.setServerName("My World v6.0");
        settings.setPlayerIdleTimeout(60);
        settings.setViewDistance(8);
        settings.setAllowList(false);
        assertEquals("QUEUED", service.updateSettings(settings).getStatus());

        McGameruleUpdateRequest rules = new McGameruleUpdateRequest();
        rules.setRules(Map.of("pvp", false, "keepinventory", true));
        assertThrows(BusinessException.class, () -> service.updateGamerules(rules));
        rules.setRules(Map.of("pvp", true));
        assertEquals("QUEUED", service.updateGamerules(rules).getStatus());

        McPermissionUpdateRequest perm = new McPermissionUpdateRequest();
        perm.setXuid("2535453759792258");
        perm.setName("Steve");
        perm.setPermission("operator");
        assertEquals("QUEUED", service.updatePermission(perm).getStatus());

        McAllowlistUpdateRequest allow = new McAllowlistUpdateRequest();
        allow.setXuid("2535453759792258");
        allow.setName("Steve");
        allow.setAllowlisted(true);
        assertEquals("QUEUED", service.updateAllowlist(allow).getStatus());

        McWorldActivateRequest activate = new McWorldActivateRequest();
        activate.setFolder("TestWorld");
        assertEquals("QUEUED", service.activateWorld(activate).getStatus());
        assertEquals("STARTING", service.adminConsole().getRunPhase());
        assertTrue(service.adminConsole().getPlayers().stream()
                .anyMatch(p -> "Steve".equals(p.getName()) && "operator".equals(p.getPermission())));
    }

    @Test
    void downloadCurrentWorld() {
        assertTrue(service.downloadWorld("TestWorld").exists());
        assertThrows(BusinessException.class, () -> service.worldIcon("TestWorld", true));
    }

    @Test
    void normalizeEpochMsRejectsYear1793AndPromotesSeconds() {
        assertEquals(0L, McBedrockServiceImpl.normalizeEpochMs(-5_586_000_000_000L));
        assertEquals(1_720_000_000_000L, McBedrockServiceImpl.normalizeEpochMs(1_720_000_000L));
        assertEquals(1_720_000_000_000L, McBedrockServiceImpl.normalizeEpochMs(1_720_000_000_000L));
    }

    @Test
    void brokenRuntimeJsonWithRunningTrueStillShowsRunning() throws Exception {
        Path runtime = home.resolve("ops").resolve("runtime.json");
        Files.createDirectories(runtime.getParent());
        Files.writeString(runtime,
                "{\"running\":true,\"phase\":\"RUNNING\",\"joinHint\":\"\"My World v6.0\"\"}\n",
                StandardCharsets.UTF_8);
        McConsoleVO admin = service.adminConsole();
        assertTrue(admin.isRunning());
        assertEquals("RUNNING", admin.getRunPhase());
    }

    @Test
    void backupTimePrefersFilenameStamp() throws Exception {
        Path backups = home.resolve("backups");
        Files.createDirectories(backups);
        Files.writeString(backups.resolve("worlds-20261016-044801.tar.gz"), "x", StandardCharsets.UTF_8);
        Files.writeString(backups.resolve("worlds-20261016-233101.tar.gz"), "x", StandardCharsets.UTF_8);
        McConsoleVO admin = service.adminConsole();
        long ms = admin.getBackups().getFirst().getCreatedEpochMs();
        assertEquals(ms, admin.getLastBackupEpochMs());
        var t = java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.of("Asia/Shanghai"));
        assertEquals(2026, t.getYear());
        assertEquals(16, t.getDayOfMonth());
        assertEquals(23, t.getHour());
        assertEquals(31, t.getMinute());
    }

    @Test
    void runningCurrentWorldUsesNowForLastModified() throws Exception {
        Path runtime = home.resolve("ops").resolve("runtime.json");
        Files.createDirectories(runtime.getParent());
        Files.writeString(runtime, "{\"running\":true,\"phase\":\"RUNNING\",\"joinHint\":\"x\"}\n",
                StandardCharsets.UTF_8);
        long before = System.currentTimeMillis() - 2000;
        long ms = service.adminConsole().getCurrentWorld().getLastModifiedEpochMs();
        assertTrue(ms >= before);
        assertTrue(ms <= System.currentTimeMillis() + 2000);
    }

    @Test
    void cloneCardUsesFolderStampForBackupAndModified() {
        service.backupNow();
        var clone = service.adminConsole().getWorlds().stream()
                .filter(w -> !w.isCurrent())
                .findFirst()
                .orElseThrow();
        assertTrue(clone.getLastBackupEpochMs() > 0);
        assertEquals(clone.getLastBackupEpochMs(), clone.getLastModifiedEpochMs());
    }

    @Test
    void uploadZipBecomesWorldCard() throws Exception {
        Path zip = home.resolve("in.mcworld");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("level.dat"));
            zos.write("stub".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("levelname.txt"));
            zos.write("Imported".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        var file = new MockMultipartFile("file", "Imported.mcworld", "application/zip", Files.readAllBytes(zip));
        assertEquals("DONE", service.uploadWorld(file).getStatus());
        assertTrue(service.adminConsole().getWorlds().stream()
                .anyMatch(w -> "Imported".equals(w.getDisplayName())));
    }

    @Test
    void uploadRejectsZipWithoutLevelDat() throws Exception {
        Path zip = home.resolve("bad.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("no".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        var file = new MockMultipartFile("file", "bad.zip", "application/zip", Files.readAllBytes(zip));
        assertThrows(BusinessException.class, () -> service.uploadWorld(file));
    }

    @Test
    void seenPlayersFromLogsFileShowInConsole() throws Exception {
        Path seen = home.resolve("ops").resolve("seen-players.json");
        Files.createDirectories(seen.getParent());
        Files.writeString(seen, """
                [{"xuid":"2535453759799999","name":"AlexOnServer","online":true}]
                """, StandardCharsets.UTF_8);
        assertTrue(service.adminConsole().getPlayers().stream()
                .anyMatch(p -> "AlexOnServer".equals(p.getName()) && "2535453759799999".equals(p.getXuid())));
        assertEquals(0, service.adminConsole().getOnlineCount());

        Files.writeString(home.resolve("ops").resolve("runtime.json"), """
                {"running":true,"phase":"RUNNING","joinHint":"My World v6.0"}
                """, StandardCharsets.UTF_8);
        assertEquals(1, service.adminConsole().getOnlineCount());
    }

    @Test
    void worldTimePrefersLastPlayedFromLevelDat() throws Exception {
        long playedSeconds = 1_720_000_000L;
        Files.write(home.resolve("data").resolve("worlds").resolve("TestWorld").resolve("level.dat"),
                bedrockLevelDat(playedSeconds));
        assertEquals(playedSeconds * 1000, McBedrockServiceImpl.readLastPlayedMs(
                home.resolve("data").resolve("worlds").resolve("TestWorld").resolve("level.dat")));
        assertEquals(playedSeconds * 1000, service.adminConsole().getCurrentWorld().getLastModifiedEpochMs());
    }

    private static byte[] bedrockLevelDat(long lastPlayed) {
        byte[] name = "LastPlayed".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer nbt = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
        nbt.put((byte) 10);
        nbt.putShort((short) 0);
        nbt.put((byte) 4);
        nbt.putShort((short) name.length);
        nbt.put(name);
        nbt.putLong(lastPlayed);
        nbt.put((byte) 0);
        byte[] body = Arrays.copyOf(nbt.array(), nbt.position());
        ByteBuffer file = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(8);
        file.putInt(body.length);
        file.put(body);
        return file.array();
    }
}
