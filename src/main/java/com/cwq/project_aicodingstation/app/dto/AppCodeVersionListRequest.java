package com.cwq.project_aicodingstation.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 列出版本快照。缺的快照会在这次请求里补上。
 */
@Data
public class AppCodeVersionListRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long appId;

    @NotNull
    private Long sessionId;
}
