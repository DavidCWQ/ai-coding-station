package com.cwq.project_aicodingstation.stats.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单次计入浏览量的访问明细。心跳和被去重的重复浏览不写这张表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("visit_log")
public class VisitLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private LocalDateTime visitedAt;
    private LocalDate statDate;
    private String scope;
    private Long appId;
    private String path;
    private String visitorHash;
    private String sessionHash;
}
