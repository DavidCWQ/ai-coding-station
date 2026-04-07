package com.cwq.project_aicodingstation.agent.entity;

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
 * 智能体单条消息实体（与会话、用户关联）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("agent_chat_message")
public class AgentChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花算法）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 所属用户 id（与会话 owner 一致）
     */
    private Long userId;

    /**
     * 所属会话 id
     */
    private Long sessionId;

    /**
     * 智能体编码（冗余字段，便于排错与统计）
     */
    private String agentCode;

    /**
     * 消息正文
     */
    private String message;

    /**
     * 消息类型：user / ai / system（与 {@link com.cwq.project_aicodingstation.chat.enums.MessageTypeEnum} 取值对齐）
     */
    private String messageType;

    /**
     * 扩展 JSON（可选）
     */
    private String metadata;

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
