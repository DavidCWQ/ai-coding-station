package com.cwq.project_aicodingstation.agent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AgentShareCreatedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;

    private String title;
}
