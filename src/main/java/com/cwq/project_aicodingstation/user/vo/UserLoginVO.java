package com.cwq.project_aicodingstation.user.vo;

/*
 * 无论是用户注册还是用户登录接口，都应该返回`已登录的用户信息`，且一定要؜对返回结果进行`脱敏处理`，
 * 不能直接将数据库查到的所有信息都返回给了前端（包括密码）。
 * */

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserLoginVO implements Serializable {

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

    /// 用户角色：user/admin
    private String userRole;

    /// 创建时间
    private LocalDateTime createTime;

    /// 更新时间
    private LocalDateTime updateTime;

}
