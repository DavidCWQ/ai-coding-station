package com.cwq.project_aicodingstation.app.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.io.Serial;

/**
 * @apiNote App Entity Layer
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app")
public class App implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private String appName;
    private String cover;
    private String initPrompt;
    private String codeGenType;
    private String deployKey;

    private Long userId;
    private Integer priority;
    private LocalDateTime deployedTime;

    private LocalDateTime editTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDeleted;
}