package com.cwq.project_aicodingstation.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MessageVoteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * app 或 agent
     */
    @NotBlank(message = "来源不能为空")
    private String source;

    @NotNull(message = "消息 id 不能为空")
    private Long messageId;

    /**
     * 1 有帮助，-1 没帮助，0 取消
     */
    @NotNull(message = "评价不能为空")
    private Integer vote;

    @Size(max = 200, message = "原因最多 200 字")
    private String reason;
}
