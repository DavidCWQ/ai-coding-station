package com.cwq.project_aicodingstation.mc.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McWorldVO {
    private String folder;
    private String displayName;
    private boolean current;
    private long sizeBytes;
    private long lastModifiedEpochMs;
    private long lastBackupEpochMs;
    private boolean hasIcon;
    private String iconUrl;
}
