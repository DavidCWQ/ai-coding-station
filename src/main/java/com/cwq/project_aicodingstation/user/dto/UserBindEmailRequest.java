package com.cwq.project_aicodingstation.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserBindEmailRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// 邮箱。留空表示解除绑定。
    private String userEmail;

    /// 当前登录密码，用于确认是本人操作
    private String password;
}
