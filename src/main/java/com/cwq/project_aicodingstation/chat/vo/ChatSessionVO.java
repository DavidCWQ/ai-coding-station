package com.cwq.project_aicodingstation.chat.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话会话展示对象（不含 userId 等敏感字段）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class ChatSessionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话主键（雪花 id）
     */
    private Long id;

    /**
     * 所属应用 id
     */
    private Long appId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 最后一条消息时间（用于列表排序）
     */
    private LocalDateTime lastMsgTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
