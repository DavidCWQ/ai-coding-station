package com.cwq.project_aicodingstation.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long id;

    @Size(max = 256)
    private String appName;

    /**
     * Code generation type (STATIC_SITE / JS / VUE / etc.)
     */
    private String codeGenType;
}
