package com.cwq.project_aicodingstation.chat.export;

/**
 * 一次 Markdown 导出的文件名和正文。
 */
public record MarkdownFile(String filename, byte[] body) {
}
