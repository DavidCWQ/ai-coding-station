package com.cwq.project_aicodingstation.mc.dto;

import lombok.Data;

@Data
public class McAllowlistUpdateRequest {
    private String xuid;
    private String name;
    private boolean allowlisted;
}
