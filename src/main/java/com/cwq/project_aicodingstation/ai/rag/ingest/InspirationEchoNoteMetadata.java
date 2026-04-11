package com.cwq.project_aicodingstation.ai.rag.ingest;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import dev.langchain4j.data.document.Metadata;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 「灵感回声」语料约定：文件前若干条非空行中，若出现以 {@code 序号 + 空白 + 四字 kind} 开头的行，则解析为分类。
 * <p>
 * 例：首行直接 {@code 7 读书笔记}；或先写 {@code # 标题}，下一行 {@code 12 随笔随记}。同主题多条短句可人工合并为一个 .md 再入库。
 * </p>
 */
public final class InspirationEchoNoteMetadata {

    private static final Pattern KIND_HEAD = Pattern.compile("^\\s*(\\d+)\\s+([\\u4e00-\\u9fff]{4})");

    /** 最多扫描多少条非空行以寻找 kind 行（跳过纯标题、空行） */
    private static final int MAX_NON_EMPTY_LINES_TO_SCAN = 12;

    private InspirationEchoNoteMetadata() {
    }

    /**
     * 为 {@code inspiration_echo} 语料补充 {@code note_id}、{@code note_seq}、{@code kind}（解析成功时）。
     */
    public static void apply(Metadata metadata, String corpus, String fileName, String text) {
        if (metadata == null || !AgentCodeEnum.INSPIRATION_ECHO.getCode().equals(corpus)) {
            return;
        }
        String noteId = stripMdExtension(fileName);
        if (!noteId.isBlank()) {
            metadata.put("note_id", noteId);
        }
        String kindLine = findKindHeaderLine(text);
        if (kindLine == null) {
            return;
        }
        Matcher m = KIND_HEAD.matcher(kindLine);
        if (m.find()) {
            metadata.put("note_seq", m.group(1));
            metadata.put("kind", m.group(2));
        }
    }

    static String stripMdExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        String n = fileName.trim();
        if (n.toLowerCase(Locale.ROOT).endsWith(".md")) {
            return n.substring(0, n.length() - ".md".length());
        }
        return n;
    }

    static String findKindHeaderLine(String text) {
        if (text == null) {
            return null;
        }
        String t = text.replaceFirst("^\uFEFF", "").stripLeading();
        int nonEmpty = 0;
        for (String line : t.split("\\R", -1)) {
            String s = line.strip();
            if (s.isEmpty()) {
                continue;
            }
            nonEmpty++;
            if (nonEmpty > MAX_NON_EMPTY_LINES_TO_SCAN) {
                break;
            }
            if (KIND_HEAD.matcher(s).find()) {
                return s;
            }
        }
        return null;
    }
}
