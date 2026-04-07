package com.cwq.project_aicodingstation.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建智能体会话请求。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class AgentSessionAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体编码（如 code_assistant）
     */
    @NotBlank(message = "智能体编码不能为空")
    @Size(max = 32, message = "智能体编码过长")
    private String agentCode;

    /**
     * 会话标题，可空（服务端使用默认标题）
     */
    @Size(max = 256, message = "标题长度不能超过 256")
    private String title;
}
