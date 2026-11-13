package com.cwq.project_aicodingstation.agent.entity;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("agent_chat_share")
public class AgentChatShare implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private Long userId;

    private Long sessionId;

    private String agentCode;

    private String token;

    private String title;

    /**
     * 0 有效，1 已停止
     */
    private Integer revoked;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
