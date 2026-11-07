package com.cwq.project_aicodingstation.mc.dto;

import lombok.Data;

@Data
public class McPackToggleRequest {
    private String id;
    /** 要改哪张存档。附加包按世界生效。 */
    private String world;
    private boolean enabled;
}
