package com.cwq.project_aicodingstation.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史游标分页查询请求（按应用 + 会话隔离，向前加载更早消息）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class ChatHistoryQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 id（数据隔离维度）
     */
    @NotNull(message = "应用 id 不能为空")
    private Long appId;

    /**
     * 会话 id
     */
    @NotNull(message = "会话 id 不能为空")
    private Long sessionId;

    /**
     * 游标：在此消息 id 之前（更早）的消息；为空表示从最新一页开始
     */
    private Long beforeMessageId;

    /**
     * 游标：在此时间之前的消息（可选，优先级低于 beforeMessageId）
     */
    private LocalDateTime beforeCreateTime;

    /**
     * 每页条数，默认 10
     */
    private Integer pageSize = 10;
}
