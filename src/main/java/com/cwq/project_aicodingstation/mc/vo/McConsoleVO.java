package com.cwq.project_aicodingstation.mc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class McConsoleVO {
    private boolean configured;
    private String message;
    /** ADMIN or VISITOR */
    private String viewerRole;
    private boolean running;
    /** SLEEPING / STARTING / RUNNING / STOPPING / BACKING_UP */
    private String runPhase;
    private String serverName;
    private String joinHint;
    private McWorldVO currentWorld;
    private List<McWorldVO> worlds;
    private McSettingsVO settings;
    private List<McGameruleVO> gamerules;
    private List<McPlayerVO> players;
    private List<McBackupVO> backups;
    private Long lastBackupEpochMs;
    private String pendingOpId;
    private String pendingOpAction;
}
