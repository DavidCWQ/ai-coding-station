package com.cwq.project_aicodingstation.agent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AgentShareView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;

    private String title;

    private String agentCode;

    private String agentTitle;

    private List<Item> messages;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String role;

        private String content;
    }
}
