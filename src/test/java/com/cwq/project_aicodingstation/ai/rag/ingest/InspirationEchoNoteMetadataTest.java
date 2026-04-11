package com.cwq.project_aicodingstation.ai.rag.ingest;

import com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InspirationEchoNoteMetadataTest {

    @Test
    void findKindHeaderLine_skipsLeadingBlank() {
        assertEquals("3 原文摘抄", InspirationEchoNoteMetadata.findKindHeaderLine("\n\n  3 原文摘抄\n正文"));
    }

    @Test
    void findKindHeaderLine_afterMarkdownTitle() {
        String md = "# 关于耐心\n\n5 碎片随记\n\n想到哪写到哪。";
        assertEquals("5 碎片随记", InspirationEchoNoteMetadata.findKindHeaderLine(md));
    }

    @Test
    void stripMdExtension() {
        assertEquals("关于耐心", InspirationEchoNoteMetadata.stripMdExtension("关于耐心.md"));
        assertEquals("NOTE", InspirationEchoNoteMetadata.stripMdExtension("NOTE.MD"));
    }

    @Test
    void apply_setsNoteIdKindAndSeq_forInspirationCorpus() {
        Metadata m = new Metadata();
        m.put("corpus", AgentCodeEnum.INSPIRATION_ECHO.getCode());
        String body = "12 读书笔记\n\n想到哪写到哪。";
        InspirationEchoNoteMetadata.apply(m, AgentCodeEnum.INSPIRATION_ECHO.getCode(), "vol-01.md", body);
        assertEquals("vol-01", m.getString("note_id"));
        assertEquals("12", m.getString("note_seq"));
        assertEquals("读书笔记", m.getString("kind"));
    }

    @Test
    void apply_noKind_whenFirstLineDoesNotMatch() {
        Metadata m = new Metadata();
        InspirationEchoNoteMetadata.apply(
                m,
                AgentCodeEnum.INSPIRATION_ECHO.getCode(),
                "x.md",
                "# 只有标题\n\n无序号行");
        assertEquals("x", m.getString("note_id"));
        assertNull(m.getString("kind"));
    }

    @Test
    void apply_skipped_forOtherCorpus() {
        Metadata m = new Metadata();
        m.put("corpus", "code_assistant");
        InspirationEchoNoteMetadata.apply(m, "code_assistant", "a.md", "1 读书笔记\n");
        assertNull(m.getString("note_id"));
    }
}
