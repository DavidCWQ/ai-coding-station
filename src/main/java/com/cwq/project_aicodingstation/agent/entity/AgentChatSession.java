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
 * 智能体会话实体（元信息；消息明细见 {@link AgentChatMessage}）。
 *
 * @author <a href="https://github.com/DavidCWQ">DavidCWQ</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("agent_chat_session")
public class AgentChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花算法）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 会话所属用户 id
     */
    private Long userId;

    /**
     * 智能体编码（与 {@link com.cwq.project_aicodingstation.agent.enums.AgentCodeEnum} 一致）
     */
    private String agentCode;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 最后一条消息时间（列表排序）
     */
    private LocalDateTime lastMsgTime;

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
