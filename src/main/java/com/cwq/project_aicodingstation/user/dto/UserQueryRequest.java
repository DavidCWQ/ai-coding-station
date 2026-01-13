package com.cwq.project_aicodingstation.user.dto;

// 继承公共包中的 PageRequest 来支持分页查询
import com.cwq.project_aicodingstation.common.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String userName;

    private String userAccount;

    private String userProfile;

    private String userRole;
}
