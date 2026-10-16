package com.cwq.project_aicodingstation.mc.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McPlayerVO {
    private String name;
    private String xuid;
    /** visitor / member / operator */
    private String permission;
    private boolean allowlisted;
}
