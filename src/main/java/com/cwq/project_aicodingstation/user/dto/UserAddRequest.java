package com.cwq.project_aicodingstation.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userName;

    private String userAccount;

    private String userAvatar;

    private String userProfile;

    private String userRole;
}
