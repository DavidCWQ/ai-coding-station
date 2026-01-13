package com.cwq.project_aicodingstation.user.entity;

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

/*
 * 工程规范 ≠ 文件数量优化：
 * 在 production 代码里，`package数量` 不重要，
 * 重要的是：是否可扩展？是否职责清晰？是否一致？
 * 没有任何一个成熟项目会因为 “这个 package 现在只有一个类” 而不建。
 * 哪怕每个目录只有一个类：一眼就知道职责，新人加入成本极低，不会推翻结构重构（该文件，移 import）。
 * */

/**
 * @apiNote Sys_User Entity Layer
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // id 默认是连续生成的，容易被爬虫抓取，所以更换策略为 ASSIGN_ID 雪花算法生成
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private String userAccount;
    private String userPassword;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;

    private LocalDateTime editTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDeleted;

    private String vipCode;
    private Long vipNumber;
    private LocalDateTime vipExpireTime;

}
