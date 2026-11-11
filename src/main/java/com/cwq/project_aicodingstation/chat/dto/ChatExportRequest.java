package com.cwq.project_aicodingstation.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ChatExportRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "应用 id 不能为空")
    private Long appId;

    @NotNull(message = "会话 id 不能为空")
    private Long sessionId;

    @NotNull(message = "消息 id 不能为空")
    private Long untilMessageId;
}
