package com.cwq.project_aicodingstation.ai.tool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class InterviewQuestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String title;
    private String url;
    private String source;
}
