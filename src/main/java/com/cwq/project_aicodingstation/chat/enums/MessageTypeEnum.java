package com.cwq.project_aicodingstation.chat.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 聊天消息类型枚举（与数据库存储的 message_type 字符串一致）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Getter
public enum MessageTypeEnum {

    USER("user"), // 用户发送的消息
    AI("ai"), // AI 回复的消息（含失败时携带错误说明的 AI 消息）
    SYSTEM("system"); // 系统消息

    private final String value; // 写入数据库的取值

    MessageTypeEnum(String value) {
        this.value = value;
    }

    /**
     * 根据 value 解析枚举
     *
     * @param value 消息类型字符串
     * @return 对应枚举，无法识别时返回 null
     */
    public static MessageTypeEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (MessageTypeEnum e : MessageTypeEnum.values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }
}
