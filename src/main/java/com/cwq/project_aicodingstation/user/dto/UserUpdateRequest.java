package com.cwq.project_aicodingstation.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// 用户 id
    private Long id;

    /// 用户名称
    private String userName;

    /// 用户头像
    private String userAvatar;

    /// 用户简介
    private String userProfile;

    /// 用户角色：user/admin/vip
    private String userRole;
}
