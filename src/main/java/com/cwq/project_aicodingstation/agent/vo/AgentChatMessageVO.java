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
     * 思考过程。不进入下一轮上下文。
     */
    private String reasoning;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 当前用户的评价：1 有帮助，-1 没帮助，空表示未评价。
     */
    private Integer vote;
}
