package com.cwq.project_aicodingstation.user.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// 用户 id
    private Long id;

    /// 账号
    private String userAccount;

    /// 用户名
    private String userName;

    /// 用户头像
    private String userAvatar;

    /// 用户简介
    private String userProfile;

    /// 用户角色：user/admin/vip
    private String userRole;

    /// 创建时间
    private LocalDateTime createTime;

}

