package com.cwq.project_aicodingstation.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserResetPasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// 邮件中的重置令牌
    private String token;

    /// 新密码
    private String newPassword;

    /// 确认新密码
    private String checkNewPassword;
}
