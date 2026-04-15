package com.cwq.project_aicodingstation.app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaticResourceControllerTest {

    @TempDir
    Path previewRoot;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StaticResourceController controller = new StaticResourceController();
        ReflectionTestUtils.setField(controller, "previewRootDir", previewRoot.toString());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void serveStaticResource_returnsHtmlFileWithUtf8ContentType() throws Exception {
        Path dir = Files.createDirectories(previewRoot.resolve("demo"));
        Files.writeString(dir.resolve("index.html"), "<h1>ok</h1>", StandardCharsets.UTF_8);

        mockMvc.perform(get("/static/demo/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/html; charset=UTF-8"))
                .andExpect(content().string("<h1>ok</h1>"));
    }

    @Test
    void serveStaticResource_redirectsToSlashForDirectoryRoot() throws Exception {
        mockMvc.perform(get("/static/demo"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/static/demo/"));
    }

    @Test
    void serveStaticResource_returnsNotFoundForMissingFile() throws Exception {
        mockMvc.perform(get("/static/demo/missing.js"))
                .andExpect(status().isNotFound());
    }
}
