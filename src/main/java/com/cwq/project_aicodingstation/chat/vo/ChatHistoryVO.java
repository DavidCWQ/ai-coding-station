package com.cwq.project_aicodingstation.chat.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史展示对象（不含 userId 等敏感字段）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class ChatHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息主键
     */
    private Long id;

    /**
     * 消息正文（AI 失败时可为错误说明）
     */
    private String message;

    /**
     * 消息类型：user / ai / system
     */
    private String messageType;

    /**
     * 附件/文件列表 JSON（若有）
     */
    private String fileList;

    /**
     * 所属应用 id
     */
    private Long appId;

    /**
     * 所属会话 id
     */
    private Long sessionId;

    /**
     * 父消息 id（重试/线程上下文）
     */
    private Long parentId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
