package com.cwq.project_aicodingstation.mc.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McSettingsVO {
    private String serverName;
    private String levelName;
    private String gamemode;
    private String difficulty;
    private int maxPlayers;
    private int viewDistance;
    private int tickDistance;
    private int playerIdleTimeout;
    private boolean allowList;
    private boolean allowCheats;
    private boolean onlineMode;
}
