package com.cwq.project_aicodingstation.stats.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatsTrendPointVO {

    private LocalDate date;
    private long pv;
    private long uv;
}
