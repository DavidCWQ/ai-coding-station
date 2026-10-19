package com.cwq.project_aicodingstation.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserForgotPasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// 登录账号
    private String userAccount;
}
