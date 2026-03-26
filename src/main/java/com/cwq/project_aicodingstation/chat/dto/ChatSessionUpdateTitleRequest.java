package com.cwq.project_aicodingstation.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改会话标题请求。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class ChatSessionUpdateTitleRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 id
     */
    @NotNull(message = "会话 id 不能为空")
    private Long sessionId;

    /**
     * 新标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 256, message = "标题长度不能超过 256")
    private String title;
}
