package com.cwq.project_aicodingstation.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建对话会话请求（进入应用默认会话或用户主动「新聊天」）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
public class ChatSessionAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属应用 id
     */
    @NotNull(message = "应用 id 不能为空")
    private Long appId;

    /**
     * 会话标题，可空（由服务端生成默认标题）
     */
    @Size(max = 256, message = "标题长度不能超过 256")
    private String title;
}
