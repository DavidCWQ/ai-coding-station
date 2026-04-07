package com.cwq.project_aicodingstation.agent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体会话展示对象。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class AgentChatSessionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 智能体编码
     */
    private String agentCode;

    /**
     * 标题
     */
    private String title;

    /**
     * 最后消息时间
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
