package com.cwq.project_aicodingstation.chat.export;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationMarkdownTest {

    @Test
    void renderKeepsCodeAndStopsBeforeLaterTurns() {
        LocalDateTime at = LocalDateTime.of(2026, 11, 11, 15, 4);
        String md = ConversationMarkdown.render(
                "待办清单",
                "应用「待办清单」",
                at,
                List.of(
                        new ConversationMarkdown.Turn("user", "做一个待办", null),
                        new ConversationMarkdown.Turn("ai", "```html\n<h1>待办</h1>\n```", "先写标题\n再写列表"),
                        new ConversationMarkdown.Turn("system", "   ", null)
                ),
                true,
                false
        );

        assertTrue(md.startsWith("# 待办清单\n"));
        assertTrue(md.contains("- 导出于 2026-11-11 15:04"));
        assertTrue(md.contains("- 来源：应用「待办清单」"));
        assertTrue(md.contains("### 你\n\n做一个待办"));
        assertTrue(md.contains("```html\n<h1>待办</h1>\n```"));
        assertTrue(md.contains("> 思考\n>\n> 先写标题\n> 再写列表"));
        assertFalse(md.contains("### 系统"));
    }

    @Test
    void personalDownloadCanOmitReasoning() {
        String md = ConversationMarkdown.render(
                "对话",
                "智能体「编程助手」",
                LocalDateTime.of(2026, 1, 2, 3, 4),
                List.of(new ConversationMarkdown.Turn("assistant", "你好", "秘密")),
                false,
                true
        );
        assertTrue(md.contains("更早的消息没有写入这份文件"));
        assertTrue(md.contains("### 助手\n\n你好"));
        assertFalse(md.contains("秘密"));
    }

    @Test
    void filenameStripsUnsafeCharacters() {
        String name = ConversationMarkdown.filename("a/b:c", LocalDateTime.of(2026, 11, 11, 9, 8));
        assertEquals("a_b_c-对话-20261111-0908.md", name);
    }
}
