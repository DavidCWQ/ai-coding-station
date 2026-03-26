package com.cwq.project_aicodingstation.chat.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史实体（单条消息，与应用、会话、用户关联）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_history")
public class ChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花算法）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型：user / ai / system（见 {@link com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum}）
     */
    private String messageType;

    /**
     * 关联文件列表（JSON 字符串）
     */
    private String fileList;

    /**
     * 所属应用 id
     */
    private Long appId;

    /**
     * 发送/产生该条记录的用户 id
     */
    private Long userId;

    /**
     * 所属会话 id
     */
    private Long sessionId;

    /**
     * 父消息 id（上下文/重试）
     */
    private Long parentId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0 未删除，1 已删除
     */
    @Column(isLogicDelete = true)
    private Integer isDeleted;
}
