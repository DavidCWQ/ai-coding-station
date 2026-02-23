package com.cwq.project_aicodingstation.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppAdminUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long id;

    @Size(max = 256)
    private String appName;

    @Size(max = 512)
    private String cover;

    /**
     * Priority (99 = featured, 999 = pinned)
     */
    private Integer priority;

    /**
     * Create user id
     */
    private Long userId;
}
