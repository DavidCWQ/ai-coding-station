package com.cwq.project_aicodingstation.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 游标分页查询某智能体会话下的历史消息（时间正序返回）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class AgentHistoryQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体编码（与会话一致）
     */
    @NotBlank(message = "智能体编码不能为空")
    @Size(max = 32, message = "智能体编码过长")
    private String agentCode;

    /**
     * 会话 id
     */
    @NotNull(message = "会话 id 不能为空")
    private Long sessionId;

    /**
     * 取比该消息更早的记录（与 chat 模块游标语义一致）
     */
    private Long beforeMessageId;

    /**
     * 兼容：早于该时间的记录
     */
    private LocalDateTime beforeCreateTime;

    /**
     * 每页条数
     */
    private Integer pageSize;
}
