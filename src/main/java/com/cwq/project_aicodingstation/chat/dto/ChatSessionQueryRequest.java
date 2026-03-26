package com.cwq.project_aicodingstation.chat.dto;

import com.cwq.project_aicodingstation.common.request.PageRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话列表查询请求（某应用下当前用户的会话，按最后消息时间倒序分页）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatSessionQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    @NotNull(message = "应用 id 不能为空")
    private Long appId;
}
