package com.cwq.project_aicodingstation.mc.dto;

import lombok.Data;

import java.util.Map;

@Data
public class McGameruleUpdateRequest {
    /** gamerule id -> true/false */
    private Map<String, Boolean> rules;
}
