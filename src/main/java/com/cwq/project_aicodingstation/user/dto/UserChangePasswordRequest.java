package com.cwq.project_aicodingstation.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserChangePasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String oldPassword;

    private String newPassword;

    private String checkNewPassword;
}
