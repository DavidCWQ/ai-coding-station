package com.cwq.project_aicodingstation.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AppReplaceImageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long appId;

    @NotBlank
    private String newImagePath;

    private String targetXpath;

    private String targetTag;

    private String targetId;

    private List<String> targetClassList;

    private List<String> targetTagPath;
}
