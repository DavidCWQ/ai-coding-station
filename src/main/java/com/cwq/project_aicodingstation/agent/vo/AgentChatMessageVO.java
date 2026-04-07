package com.cwq.project_aicodingstation.agent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体消息展示对象。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class AgentChatMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 会话 id
     */
    private Long sessionId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 正文
     */
    private String message;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
