package com.cwq.project_aicodingstation.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppChatGenCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long appId;

    @NotNull
    private Long sessionId;

    @NotBlank
    private String message;
}
