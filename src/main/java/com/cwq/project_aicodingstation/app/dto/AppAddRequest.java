package com.cwq.project_aicodingstation.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 256)
    private String appName;

    @Size(max = 512)
    private String cover;

    /**
     * Initial prompt for AI to generate the website
     */
    private String initPrompt;

    /**
     * Code generation type (STATIC_SITE / JS / VUE / etc.)
     */
    private String codeGenType;
}
