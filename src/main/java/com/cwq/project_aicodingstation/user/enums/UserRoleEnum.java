package com.cwq.project_aicodingstation.user.enums;

import cn.hutool.core.util.ObjUtil;
import com.cwq.project_aicodingstation.user.constant.UserConstant;
import lombok.Getter;

@Getter
public enum UserRoleEnum {

    VIP("会员", UserConstant.VIP_ROLE),
    USER("用户", UserConstant.DEFAULT_ROLE),
    ADMIN("管理员", UserConstant.ADMIN_ROLE);

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
