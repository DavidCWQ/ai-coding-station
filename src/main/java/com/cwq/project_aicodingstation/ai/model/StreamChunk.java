package com.cwq.project_aicodingstation.ai.model;

/**
 * 一次模型流式增量。思考和正文分开，避免思考被写进代码或下一轮上下文。
 */
public record StreamChunk(boolean reasoning, String text) {

    public static StreamChunk content(String text) {
        return new StreamChunk(false, text == null ? "" : text);
    }

    public static StreamChunk reasoning(String text) {
        return new StreamChunk(true, text == null ? "" : text);
    }
}
