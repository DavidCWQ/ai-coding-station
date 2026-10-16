package com.cwq.project_aicodingstation.mc.dto;

import lombok.Data;

@Data
public class McPermissionUpdateRequest {
    private String xuid;
    private String name;
    /** visitor / member / operator */
    private String permission;
}
