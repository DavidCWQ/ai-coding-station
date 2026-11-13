package com.cwq.project_aicodingstation.app.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeVersionFilesTest {

    @TempDir
    Path root;

    @Test
    void snapshotSkipsVersionDirAndRestoreReplacesLiveFiles() throws Exception {
        Path app = root.resolve("html_1");
        Files.createDirectories(app.resolve("img"));
        Files.writeString(app.resolve("index.html"), "new");
        Files.writeString(app.resolve("img/a.png"), "png");
        Path older = app.resolve(".versions").resolve("9");
        Files.createDirectories(older);
        Files.writeString(older.resolve("index.html"), "old");

        Path snapshot = app.resolve(".versions").resolve("10");
        CodeVersionFiles.replaceWithTreeExceptVersions(app, snapshot);

        assertEquals("new", Files.readString(snapshot.resolve("index.html")));
        assertEquals("png", Files.readString(snapshot.resolve("img/a.png")));
        assertFalse(Files.exists(snapshot.resolve(".versions")));
        assertEquals("old", Files.readString(older.resolve("index.html")));

        Files.writeString(app.resolve("index.html"), "newer");
        Files.writeString(app.resolve("extra.txt"), "x");
        CodeVersionFiles.restoreSnapshot(app, snapshot);

        assertEquals("new", Files.readString(app.resolve("index.html")));
        assertEquals("png", Files.readString(app.resolve("img/a.png")));
        assertFalse(Files.exists(app.resolve("extra.txt")));
        assertEquals("old", Files.readString(older.resolve("index.html")));
        assertEquals("new", Files.readString(snapshot.resolve("index.html")));
    }

    @Test
    void hasSavableCodeMatchesPreviewRule() {
        assertTrue(CodeVersionFiles.hasSavableCode("```html\n<div></div>\n```"));
        assertTrue(CodeVersionFiles.hasSavableCode("  <!DOCTYPE html><html></html>"));
        assertFalse(CodeVersionFiles.hasSavableCode("只是一段说明"));
    }
}
