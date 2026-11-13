package com.cwq.project_aicodingstation.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 固定、或从某一版继续改。
 */
@Data
public class AppCodeVersionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long appId;

    @NotNull
    private Long sessionId;

    @NotNull
    private Long messageId;
}
