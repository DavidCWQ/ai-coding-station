package com.cwq.project_aicodingstation.agent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AgentShareCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "会话 id 不能为空")
    private Long sessionId;

    @NotEmpty(message = "请选择要分享的对话")
    private List<Long> messageIds;
}
