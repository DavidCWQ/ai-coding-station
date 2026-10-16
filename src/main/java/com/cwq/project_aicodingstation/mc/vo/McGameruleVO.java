package com.cwq.project_aicodingstation.mc.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McGameruleVO {
    private String id;
    private String label;
    private String hint;
    private boolean hot;
    private Boolean value;
}
