package com.cwq.project_aicodingstation.app.dto;

import com.cwq.project_aicodingstation.common.request.PageRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long id;

    @Size(max = 256)
    private String appName;

    @Size(max = 512)
    private String cover;

    /**
     * Initial prompt for AI to generate the website
     */
    private String initPrompt;

    /**
     * Code generation type filter
     */
    private String codeGenType;

    /**
     * Deployment key (unique)
     */
    private String deployKey;

    /**
     * Priority (99 = featured, 999 = pinned)
     */
    private Integer priority;

    /**
     * Create user id
     */
    private Long userId;
}
