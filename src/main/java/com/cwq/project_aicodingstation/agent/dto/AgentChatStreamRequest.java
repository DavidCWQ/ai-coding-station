package com.cwq.project_aicodingstation.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能体流式对话请求（POST SSE）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class AgentChatStreamRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 id
     */
    @NotNull(message = "会话 id 不能为空")
    private Long sessionId;

    /**
     * 智能体编码（须与会话一致）
     */
    @NotBlank(message = "智能体编码不能为空")
    @Size(max = 32, message = "智能体编码过长")
    private String agentCode;

    /**
     * 用户输入
     */
    @NotBlank(message = "消息不能为空")
    @Size(max = 32000, message = "消息过长")
    private String message;
}
