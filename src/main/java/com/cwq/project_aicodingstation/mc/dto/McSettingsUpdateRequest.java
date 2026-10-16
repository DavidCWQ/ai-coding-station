package com.cwq.project_aicodingstation.mc.dto;

import lombok.Data;

@Data
public class McSettingsUpdateRequest {
    private String serverName;
    private String gamemode;
    private String difficulty;
    private Integer maxPlayers;
    private Integer viewDistance;
    private Integer tickDistance;
    private Integer playerIdleTimeout;
    private Boolean allowList;
}
