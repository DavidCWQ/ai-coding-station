package com.cwq.project_aicodingstation.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询某智能体下当前用户的会话列表。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class AgentSessionQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体编码
     */
    @NotBlank(message = "智能体编码不能为空")
    @Size(max = 32, message = "智能体编码过长")
    private String agentCode;

    /**
     * 页码，从 1 开始
     */
    private Long pageNum = 1L;

    /**
     * 每页条数
     */
    private Long pageSize = 10L;
}
