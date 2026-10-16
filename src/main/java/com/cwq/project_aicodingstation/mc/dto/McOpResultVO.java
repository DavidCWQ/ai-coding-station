package com.cwq.project_aicodingstation.mc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McOpResultVO {
    private String opId;
    private String action;
    /** QUEUED / RUNNING / DONE / FAILED */
    private String status;
    private String message;
}
