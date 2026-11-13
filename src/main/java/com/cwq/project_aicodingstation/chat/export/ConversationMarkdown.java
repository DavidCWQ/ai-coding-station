package com.cwq.project_aicodingstation.chat.export;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 把一轮轮对话写成 Markdown。应用导出和智能体「下载所选」使用同一版式。
 */
public final class ConversationMarkdown {

    private static final DateTimeFormatter EXPORTED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_AT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    public static final int MAX_TURNS = 2000;

    private ConversationMarkdown() {
    }

    public record Turn(String role, String content, String reasoning) {
    }

    public static String render(String title, String source, LocalDateTime exportedAt,
                                List<Turn> turns, boolean includeReasoning, boolean truncated) {
        String safeTitle = StrUtil.blankToDefault(StrUtil.trim(title), "对话");
        String when = exportedAt == null ? "" : exportedAt.format(EXPORTED_AT);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(safeTitle).append("\n\n");
        sb.append("- 导出于 ").append(when).append('\n');
        sb.append("- 来源：").append(source == null ? "" : source).append('\n');
        if (truncated) {
            sb.append("- 更早的消息没有写入这份文件\n");
        }
        sb.append("\n---\n");
        if (turns != null) {
            for (Turn turn : turns) {
                appendTurn(sb, turn, includeReasoning);
            }
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }
        return sb.toString();
    }

    public static String filename(String name, LocalDateTime at) {
        String base = StrUtil.blankToDefault(StrUtil.trim(name), "对话");
        base = base.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        LocalDateTime when = at == null ? LocalDateTime.now() : at;
        return base + "-对话-" + when.format(FILE_AT) + ".md";
    }

    private static void appendTurn(StringBuilder sb, Turn turn, boolean includeReasoning) {
        if (turn == null) {
            return;
        }
        boolean hasBody = StrUtil.isNotBlank(turn.content());
        boolean hasThought = includeReasoning && StrUtil.isNotBlank(turn.reasoning());
        if (!hasBody && !hasThought) {
            return;
        }
        sb.append("\n### ").append(heading(turn.role())).append("\n\n");
        if (hasBody) {
            sb.append(turn.content().trim()).append('\n');
        }
        if (hasThought) {
            sb.append('\n').append(quoteReasoning(turn.reasoning().trim())).append('\n');
        }
    }

    private static String heading(String role) {
        if ("user".equals(role)) {
            return "你";
        }
        if ("system".equals(role)) {
            return "系统";
        }
        return "助手";
    }

    private static String quoteReasoning(String reasoning) {
        StringBuilder sb = new StringBuilder();
        sb.append("> 思考\n>\n");
        for (String line : reasoning.split("\\R", -1)) {
            sb.append("> ").append(line).append('\n');
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
