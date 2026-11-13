package com.cwq.project_aicodingstation.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ChatRewriteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "应用 id 不能为空")
    private Long appId;

    @NotNull(message = "会话 id 不能为空")
    private Long sessionId;

    @NotNull(message = "消息 id 不能为空")
    private Long userMessageId;

    /**
     * 新的用户发言。为空则沿用原文，只去掉之后的消息。
     */
    @Size(max = 32000, message = "消息过长")
    private String message;
}
