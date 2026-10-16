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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
}
