package com.cwq.project_aicodingstation.mc.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McBackupVO {
    private String filename;
    private long sizeBytes;
    private long createdEpochMs;
}
